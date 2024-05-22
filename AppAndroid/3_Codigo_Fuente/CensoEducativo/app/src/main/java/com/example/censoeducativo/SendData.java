package com.example.censoeducativo;

import androidx.appcompat.app.AppCompatActivity;

import androidx.constraintlayout.widget.Group;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;


public class SendData extends AppCompatActivity {

    private TextView lblConfirmSend, lblThanks;
    private ImageView imgArchive;

    private String idSend = null;
    private long lngWaitToMain = 10000;       // 10 segundos

    private TextView txtCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos ActionBar de la parte superior
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        // Agregamos layout de la actividad
        setContentView(R.layout.activity_senddata);

        lblConfirmSend = (TextView) findViewById(R.id.lblConfirmSend);
        lblThanks = (TextView) findViewById(R.id.lblThanks);
        imgArchive = (ImageView) findViewById(R.id.imgArchive);

        txtCheck = (TextView) findViewById(R.id.txtCheck);

        // 1. Obtenemos el código del envío
        Intent intent = getIntent();
        idSend = intent.getStringExtra("idSend");

        if (idSend != null){
            AsyncSendData asyncSendData = new AsyncSendData();
            asyncSendData.execute();
        }
        else {
            //Toast.makeText(SendData.this, "Non IdSend found", Toast.LENGTH_SHORT).show();
            Log.e("LogOut", "Non IdSend found");
        }
    }

    class AsyncSendData extends AsyncTask<String, String, String> {

        // Requiere permisos en el Manifest
        //
        // <uses-permission android:name="android.permission.INTERNET"/>
        // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
        //
        // y en la aplicación:
        // android:usesCleartextTraffic="true"
        String strError="";

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {

            String strDataSend="", strDataGet="";
            String strStatus="", strCENEDU="", strPhotoPath="", strPhoto="";
            String resp="";

            GeoDatafile geoDatafile = new GeoDatafile();
            String fileContents = geoDatafile.ReadGeoData(SendData.this);

            if (fileContents != null) {

                try {

                    JSONObject jsonObject = new JSONObject(fileContents);
                    JSONObject jsonItems = jsonObject.getJSONObject("geodata");

                    if (jsonObject != null) {

                        JSONObject objItem = jsonItems.getJSONObject(idSend);

                        if (objItem != null) {

                            strDataSend = "CodMod=" + objItem.getString("CodMod") +
                                    "&Anexo=0" +
                                    "&Latitud=" + objItem.getString("Latitud") +
                                    "&Longitud=" + objItem.getString("Longitud") +
                                    "&Precision=" + objItem.getString("Precision") +
                                    "&Altitud=" + objItem.getString("Altitud") +
                                    "&Fecha=" + objItem.getString("DateTime");

                            strPhotoPath = objItem.getString("PhotoPath");

                            if (!strPhotoPath.equals("")) {
                                strPhoto = URLEncoder.encode(ImageToString(strPhotoPath), "UTF-8");
                                //strDataSend = strDataSend + "&Foto="+strPhoto;
                            }

                            if (isInternetAvailable()) {

                                //Enviar los datos al servidor
                                try {
                                    URL url = new URL("https://sigmed.minedu.gob.pe/editor/wcf/Service.svc/RegisterData");
                                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                    urlConnection.setRequestMethod("POST");
                                    urlConnection.setDoOutput(true);
                                    urlConnection.setDoInput(true);
                                    urlConnection.setChunkedStreamingMode(0);
                                    urlConnection.setConnectTimeout(120000);        // 2 minutos de tiempo máximo de espera

                                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                                    writer.write(strDataSend);
                                    writer.flush();

                                    try {
                                        if (urlConnection.getResponseCode() == 200) {

                                            InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                                            if (inputStream != null) {
                                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                                                StringBuilder strInputStream = new StringBuilder();
                                                String strLine = "";

                                                while ((strLine = bufferedReader.readLine()) != null) {
                                                    strInputStream.append(strLine);
                                                }
                                                strDataGet = strInputStream.toString();

                                                //Borrar esto cuando se actualice el nuevo Servicio Web
                                                if (strDataGet.length() > 0) {
                                                    strDataGet = strDataGet.replace("\\", "");
                                                    strDataGet = strDataGet.substring(1);
                                                    strDataGet = strDataGet.substring(0, strDataGet.length() - 1);
                                                }
                                                //
                                            }
                                            inputStream.close();
                                        }
                                    } finally {
                                        urlConnection.disconnect();
                                    }

                                } catch (IOException e) {
                                    strError = "Error while open connection to server";
                                    Log.e("AsyncSendData", "Error while open connection to server" + e.getMessage().toString(), e);
                                    e.printStackTrace();
                                }

                                if (strDataGet.length() > 0) {
                                    try {
                                        JSONObject jsonArray = new JSONObject(strDataGet).getJSONObject("Result");
                                        if (jsonArray != null) {
                                            strStatus = jsonArray.getString("Status");
                                            strCENEDU = jsonArray.getString("CEN_EDU");

                                            if (strStatus != "") {

                                                //Confirmación de envío
                                                resp = "1";

                                                if (strStatus.equals("0")) {
                                                    strStatus = "3";
                                                }

                                                //Actualizar los datos del envío
                                                jsonObject.getJSONObject("geodata").getJSONObject(idSend).put("Send", strStatus);
                                                jsonObject.getJSONObject("geodata").getJSONObject(idSend).put("CenEdu", strCENEDU);

                                                // Actualizamos el archivo de datos
                                                geoDatafile.UpdateDatafile(SendData.this, jsonObject.toString());

                                            } else {
                                                strError = "Non status value";
                                                Log.e("AsyncSenData", "Non status value");
                                            }
                                        } else {
                                            strError = "Error reading response";
                                            Log.e("AsyncSenData", "Error reading response: ");
                                        }
                                    } catch (JSONException ex) {
                                        strError = "Error reading response";
                                        Log.e("AsyncSenData", "Error reading response: " + ex.getMessage().toString());
                                        ex.printStackTrace();
                                    }
                                }
                                else{
                                    strError = "Blank response";
                                    Log.e("AsyncSenData", "Blank response");
                                }

                            }

                        } else {
                            strError = "Non IdSend found";
                            Log.e("AsyncSenData", "Non IdSend found");
                        }
                    }

                } catch (JSONException | UnsupportedEncodingException e) {
                    strError = "Error when access geodatafile";
                    Log.e("AsyncSenData", "Error when access geodatafile: " + e.getMessage().toString());
                    e.printStackTrace();
                }
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            Group grpWait = findViewById(R.id.grpWait);
            grpWait.setVisibility(View.GONE);

            if (result.equals("1")) {
                lblConfirmSend.setText(R.string.confirm);
                lblThanks.setText(R.string.thanks);
            }
            else {
                lblConfirmSend.setText(R.string.noconfirm);
                lblThanks.setText(R.string.nexttime);
                imgArchive.setVisibility(View.VISIBLE);
            }

            txtCheck.setText(strError);                 // Pasar el mensaje a un Toast si se presentara el caso


            // Ponemos tiempo de espera para pasar a la ventana principal
            new CountDownTimer(lngWaitToMain, 1000) {

                public void onTick(long millisUntilFinished) {

                }

                public void onFinish() {
                    //Direccionamos a la ventana inicial
                    Intent intent = new Intent(SendData.this, Main.class);
                    startActivity(intent);
                    finish();
                }
            }.start();

        }

        //Repetido en MainActivityTest
        private boolean isInternetAvailable() {
            if (isNetworkAvailable()) {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) (new URL("https://sigmed.minedu.gob.pe/editor/wcf/Service.svc/CheckService?ping=1").openConnection());
                    //urlConnection.setRequestProperty("User-Agent", "Android");
                    //urlConnection.setRequestProperty("Connection", "close");
                    urlConnection.setConnectTimeout(3000);
                    urlConnection.connect();
                    return (urlConnection.getResponseCode() == 200);
                } catch (SocketTimeoutException e) {
                    Log.e("AsyncSendData", "Error timeout: " + e.getMessage().toString(), e);
                } catch (IOException e) {
                    Log.e("AsyncSendData", "Error checking service available: " + e.getMessage().toString(), e);
                }
            } else {
                Log.d("AsyncSendData", "No network available!");
            }
            return false;
        }

        //Repetido en MainActivityTest
        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            //return activeNetworkInfo != null;
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return true;
            }
            return false;
        }

    }

    private String ImageToString(String PhotoPath) {

        Bitmap BitmapPhoto = null;
        Bitmap ReducePhoto = null;
        byte[] imageBytes = null;

        try {
            File file = new File(PhotoPath);
            Uri uri = Uri.fromFile(file);
            BitmapPhoto = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
        } catch (IOException e) {
            Log.e("ImagetoString", "Error when read Photo: " + e.getMessage().toString());
            e.printStackTrace();
        }

        if (BitmapPhoto == null) {
            return "";
        } else {
            ReducePhoto = reduceImageSize(BitmapPhoto,240000);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ReducePhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            //BitmapPhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            imageBytes = baos.toByteArray();
        }

        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private Bitmap reduceImageSize(Bitmap bitmap, int MAXSIZE){

        double ratioSquare, ratio;
        int bitmapHeight, bitmapWidth;
        int reduceHeight, reduceWidth;
        bitmapHeight = bitmap.getHeight();
        bitmapWidth = bitmap.getWidth();
        ratioSquare = (bitmapHeight * bitmapWidth) / MAXSIZE;

        Log.d("ReduceImage", "Alto:" + bitmapHeight + ", Ancho:" + bitmapWidth + ", RatioCuadrado:" + ratioSquare);

        if (ratioSquare <= 1 ){
            return bitmap;
        }

        ratio = Math.sqrt(ratioSquare);
        reduceHeight = (int) Math.round(bitmapHeight / ratio);
        reduceWidth = (int) Math.round(bitmapWidth / ratio);

        return Bitmap.createScaledBitmap(bitmap, reduceWidth, reduceHeight,true);
    }

}