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
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
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
import java.util.ArrayList;

public class Initial extends AppCompatActivity {

    private ArrayList<String> arrCoordsToSend = new  ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos ActionBar de la parte superior
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        // Agregamos layout de la actividad
        setContentView(R.layout.activity_initial);

        //TextView Msg = (TextView) findViewById(R.id.lblMessage);

        GeoDatafile geoDatafile = new GeoDatafile();

        // 1. Creamos archivo de datos
        geoDatafile.CreateDatafile(this);

        // 2. Contamos número de envios pendientes
        String fileContents = geoDatafile.ReadGeoData(this);

        if (fileContents != null){
            try {

                JSONObject jsonObject = new JSONObject(fileContents);
                JSONObject jsonItems = jsonObject.getJSONObject("geodata");
                JSONArray jsonItemNames = jsonItems.names();

                for (int i=0; i<jsonItems.length(); i++){

                    JSONObject obj = jsonItems.getJSONObject(jsonItemNames.getString((i)));
                    //Msg.setText(obj.toString());
                    if (obj.getString("Send").equals("0")) {
                        arrCoordsToSend.add(jsonItemNames.getString((i)));
                    }

                }
            } catch (JSONException e) {
                Log.e("Initial", "Error when access geodatafile: " + e.getMessage().toString());
                e.printStackTrace();
            }
        }


        // 3. Si hay archivos pendientes enviamos por Internet
        if (arrCoordsToSend.size() > 0){

            String[] arrIdSend;
            arrIdSend = new String[arrCoordsToSend.size()];
            for (int i = 0; i < arrCoordsToSend.size(); i++) {
                arrIdSend[i]= arrCoordsToSend.get(i);
            }

            AsyncSendData asyncSendData = new AsyncSendData();
            asyncSendData.execute(arrIdSend);

        }
        else {

            // 4. Direccionamos a la ventana inicial
            Intent intent = new Intent(Initial.this, Main.class);
            startActivity(intent);
            finish();
        }
    }


    class AsyncSendData extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            Group grpLocationIcon = findViewById(R.id.grpLocationIcon);
            grpLocationIcon.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            String strDataSend="", strDataGet="";
            String strStatus="", strCENEDU="", strPhotoPath="", strPhoto="";

            GeoDatafile geoDatafile = new GeoDatafile();
            String fileContents = geoDatafile.ReadGeoData(Initial.this);

            if (fileContents != null){

                    try {

                        JSONObject jsonObject = new JSONObject(fileContents);
                        JSONObject jsonItems = jsonObject.getJSONObject("geodata");

                        // Para cada código pendiente
                        for (int i = 0; i < params.length; i++) {

                            String IdSend = params[i];

                            JSONObject objItem = jsonItems.getJSONObject(IdSend);

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

                                    // Enviamos los datos al servidor
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
                                        Log.e("AsyncSendData", "Error while open connection to server" + e.getMessage().toString(), e);
                                        e.printStackTrace();
                                    }


                                    // Leemos respuesta y actualizamos estado de envio
                                    if (strDataGet.length() > 0) {
                                        try {
                                            JSONObject jsonArray = new JSONObject(strDataGet).getJSONObject("Result");
                                            if (jsonArray != null) {
                                                strStatus = jsonArray.getString("Status");
                                                strCENEDU = jsonArray.getString("CEN_EDU");

                                                if (strStatus != "") {

                                                    if (strStatus.equals("0")) {
                                                        strStatus = "3";
                                                    }

                                                    //Actualizar los datos del envío
                                                    jsonObject.getJSONObject("geodata").getJSONObject(IdSend).put("Send", strStatus);
                                                    jsonObject.getJSONObject("geodata").getJSONObject(IdSend).put("CenEdu", strCENEDU);

                                                } else {
                                                    Log.e("Initial", "Non status value");
                                                }
                                            }
                                        } catch (JSONException ex) {
                                            Log.e("Initial", "Error reading response: " + ex.getMessage().toString());
                                            ex.printStackTrace();
                                        }
                                    } else {
                                        Log.e("Initial", "Blank response");
                                    }

                                }

                            } else {
                                Log.e("Initial", "Non IdSend found");
                            }
                        }


                        // Actualizamos el archivo de datos
                        geoDatafile.UpdateDatafile(Initial.this, jsonObject.toString());

                    } catch (JSONException | UnsupportedEncodingException e) {
                        Toast.makeText(Initial.this, "Error when access geodatafile", Toast.LENGTH_SHORT).show();
                        Log.e("Initial", "Error when access geodatafile: " + e.getMessage().toString());
                        e.printStackTrace();
                    }

            }
            return null;

        }

        @Override
        protected void onPostExecute(String result) {

            Group grpLocationIcon = findViewById(R.id.grpLocationIcon);
            grpLocationIcon.setVisibility(View.GONE);

            // 4. Direccionamos a la ventana inicial
            Intent intent = new Intent(Initial.this, Main.class);
            startActivity(intent);
            finish();

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
                    Log.e("Initial", "Error timeout: " + e.getMessage().toString(), e);
                } catch (IOException e) {
                    Log.e("Initial", "Error checking service available: " + e.getMessage().toString(), e);
                }
            } else {
                Log.d("Initial", "No network available!");
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

        //Repetido en LogOut
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

        //Repetido en LogOut
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

}