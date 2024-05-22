package com.example.censoeducativo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Calendar;

public class Main extends AppCompatActivity {

    public ImageView imgArchive;

    public EditText txtCodigoModular;
    public TextView txtCentroEducativo, lblBlankSpace;

    public ImageView imgLocation;
    public TextView btnLocation;
    public TextView txtLatitud, txtLongitud, txtPrecision, txtAltitud;

    public ImageView imgPhoto;
    public TextView btnPhoto;
    public ImageView imgPhotoPreview;
    private String photoPath="";
    private String photoPrefix="geo_", photoExtension = ".jpg";

    public Button btnSend;

    private String flgCode = "0";
    private Boolean flgCoords = false, flgPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos ActionBar de la parte superior
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        // Agregamos layout de la actividad
        setContentView(R.layout.activity_main);

        imgArchive = (ImageView) findViewById(R.id.imgArchive);
        btnLocation = (TextView) findViewById(R.id.lblLocation);
        btnPhoto = (TextView) findViewById(R.id.lblPhoto);
        btnSend = (Button) findViewById(R.id.btnSend);

        txtCodigoModular =(EditText) findViewById(R.id.txtCode);
        txtCentroEducativo = (TextView) findViewById(R.id.txtName);
        lblBlankSpace = (TextView) findViewById(R.id.lblBlankSpace);

        imgLocation = (ImageView) findViewById(R.id.imgLocation);
        txtLatitud = (TextView) findViewById(R.id.txtLatitud);
        txtLongitud = (TextView) findViewById(R.id.txtLongitud);
        txtPrecision = (TextView) findViewById(R.id.txtPrecision);
        txtAltitud = (TextView) findViewById(R.id.txtAltitud);

        imgPhoto = (ImageView) findViewById(R.id.imgPhoto);
        imgPhotoPreview = (ImageView) findViewById(R.id.imgPhotoPreview);

        txtCodigoModular.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                String strCODMOD = txtCodigoModular.getText().toString();

                if (strCODMOD.length() == 7 ) {
                    AsyncCheckCode asyncCheckCode = new AsyncCheckCode();
                    asyncCheckCode.execute(strCODMOD);
                }
                else {
                    if (strCODMOD.length() > 0 ) {
                        txtCentroEducativo.setText("");
                        txtCentroEducativo.setVisibility(View.GONE);
                        lblBlankSpace.setVisibility(View.VISIBLE);
                        flgCode = "0";
                    }
                }

            }
        });

        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Main.this, GPS.class);
                //Actualizar este llamado al activity GPS
                startActivityForResult(intent,1);

            }
        });

        btnPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File PhotoFile = null;
                PhotoFile = CreateImage();
                if (PhotoFile != null){
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Uri PhotoUri = FileProvider.getUriForFile(Main.this, "com.example.censoeducativo", PhotoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, PhotoUri);
                    activityresultLauncher.launch(intent);
                }

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String strCODMOD = txtCodigoModular.getText().toString();
                String strCENEDU = txtCentroEducativo.getText().toString();
                String strLatitud = txtLatitud.getText().toString();
                String strLongitud = txtLongitud.getText().toString();
                String strPrecision = txtPrecision.getText().toString();
                String strAltitud = txtAltitud.getText().toString();

                //Validacion de datos
                if (strCODMOD.length() == 0 ) {
                    Toast.makeText(Main.this, R.string.msg_check_noncode, Toast.LENGTH_SHORT).show();
                }
                else {
                    if (strCODMOD.length() != 7 ) {
                        Toast.makeText(Main.this, R.string.msg_check_nonsizecode, Toast.LENGTH_SHORT).show();
                    }
                    else{
                        if (flgCode.equals("3")){
                            Toast.makeText(Main.this, R.string.msg_check_nonvalidcode, Toast.LENGTH_SHORT).show();
                        }
                        else {
                            if (!flgCoords) {
                                Toast.makeText(Main.this, R.string.msg_check_noncoords, Toast.LENGTH_SHORT).show();
                            } else {
                                if (!flgPhoto) {
                                    Toast.makeText(Main.this, R.string.msg_check_nonphoto, Toast.LENGTH_SHORT).show();
                                } else {

                                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(Main.this);
                                    alertDialog.setCancelable(false);
                                    alertDialog.setTitle(getString(R.string.confirm_message_title, strCODMOD))
                                            .setMessage(R.string.confirm_message)
                                            .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                                                    GeoDatafile geoDatafile = new GeoDatafile();
                                                    String fileContents = geoDatafile.ReadGeoData(Main.this);

                                                    if (fileContents != null) {

                                                        try {

                                                            //Creamos ID del envío
                                                            String idSend = "";
                                                            String currentDateTime = getCurrentDateTime();

                                                            idSend = currentDateTime.replaceAll("-", "");
                                                            idSend = idSend.replaceAll(":", "");
                                                            idSend = idSend.replaceAll(" ", "");
                                                            idSend = strCODMOD + idSend;

                                                            //Creamos el nuevo objeto de datos
                                                            JSONObject jsonDataAdd = new JSONObject();
                                                            jsonDataAdd.put("DateTime", currentDateTime);
                                                            jsonDataAdd.put("CodMod", strCODMOD);
                                                            jsonDataAdd.put("CenEdu", strCENEDU);
                                                            jsonDataAdd.put("Latitud", strLatitud);
                                                            jsonDataAdd.put("Longitud", strLongitud);
                                                            jsonDataAdd.put("Precision", strPrecision);
                                                            jsonDataAdd.put("Altitud", strAltitud);
                                                            jsonDataAdd.put("PhotoPath", photoPath);
                                                            jsonDataAdd.put("Send", "0");

                                                            JSONObject jsonObject = new JSONObject(fileContents);

                                                            if (jsonObject != null) {

                                                                //Agregamos el nuevo envío a la lista
                                                                jsonObject.getJSONObject("geodata").put(idSend, jsonDataAdd);

                                                                // Actualizamos el archivo de datos
                                                                String strError = geoDatafile.UpdateDatafile(Main.this, jsonObject.toString());

                                                                if (strError.equals("0")) {
                                                                    // Enviamos los datos al Servidor
                                                                    Intent intent = new Intent(Main.this, SendData.class);
                                                                    intent.putExtra("idSend", idSend);
                                                                    startActivity(intent);
                                                                    finish();
                                                                }

                                                            } else {
                                                                Toast.makeText(Main.this, "Error when access geodatafile", Toast.LENGTH_SHORT).show();
                                                                Log.e("btnSend", "Error when access geodatafile");
                                                            }

                                                        } catch (JSONException e) {
                                                            Toast.makeText(Main.this, "Error when trying to create the new data Object", Toast.LENGTH_SHORT).show();
                                                            Log.e("btnSend", "Error when trying to create the new data Object: " + e.getMessage().toString());
                                                            //e.printStackTrace();
                                                        }
                                                    }
                                                }
                                            })
                                            .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                                    paramDialogInterface.cancel();
                                                }
                                            });
                                    alertDialog.show();

                                }
                            }
                        }
                    }
                }
            }

        });

        imgArchive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Main.this, Archive.class);
                startActivity(intent);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1){
            if (resultCode == RESULT_OK){
                String gpsLatitud = data.getStringExtra("gpsLatitud");
                String gpsLongitud = data.getStringExtra("gpsLongitud");
                String gpsPrecision = data.getStringExtra("gpsPrecision");
                String gpsAltitud = data.getStringExtra("gpsAltitud");

                txtLatitud.setText(gpsLatitud);
                txtLongitud.setText(gpsLongitud);
                txtPrecision.setText(gpsPrecision);
                txtAltitud.setText(gpsAltitud);

                int intPrecisionStatus = 0;
                int intPrecisionValue =Double.valueOf(gpsPrecision).intValue(); // Try round to fix the problem

                if (intPrecisionValue > 0){
                    if (intPrecisionValue < 12){
                        intPrecisionStatus = 1;
                    }
                    else {
                        if (intPrecisionValue < 51){
                            intPrecisionStatus = 2;
                        }
                        else {
                            intPrecisionStatus = 3;
                        }
                    }
                }
                imgLocation.setImageLevel(intPrecisionStatus);

                flgCoords = true;
            }
        }
    }

    private File CreateImage() {

        String prefixImageName = photoPrefix;
        String suffixImageName = photoExtension;
        File ImageFile = null;

        File ImagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            ImageFile = File.createTempFile(prefixImageName, suffixImageName,ImagePath);
            photoPath = ImageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("btnPhoto", "Error when trying to create the Photo" + e.getMessage().toString());
            e.printStackTrace();
        }

        return ImageFile;
    }

    private Bitmap getRotate(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return rotatedBitmap;
    }

    ActivityResultLauncher<Intent> activityresultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {

                @Override
                public void onActivityResult(ActivityResult result){

                    if (result.getResultCode() == Activity.RESULT_OK){

                        Bitmap PhotoBitmap = BitmapFactory.decodeFile(photoPath);

                        if (PhotoBitmap != null) {

                            // Para rotar la imagen en memoria si esta en vertical
                            if (PhotoBitmap.getHeight() > PhotoBitmap.getWidth()) {
                                PhotoBitmap = getRotate(PhotoBitmap);
                            }

                            imgPhotoPreview.setImageBitmap(PhotoBitmap);
                            //imgPhoto.setImageLevel(1);
                            flgPhoto = true;
                        }
                        /*
                        else{
                            Log.e("btnPhoto", "Error when trying to access the Photo");
                        }*/
                    }
                }

            }
    );


    private String getCurrentDateTime() {

        String strDateTime = "";
        Calendar currentCalendar = Calendar.getInstance();

        Integer intDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        Integer intMonth = currentCalendar.get(Calendar.MONTH);
        Integer intYear = currentCalendar.get(Calendar.YEAR);
        Integer intHours = currentCalendar.get(Calendar.HOUR_OF_DAY);
        Integer intMinutes = currentCalendar.get(Calendar.MINUTE);
        Integer intSeconds = currentCalendar.get(Calendar.SECOND);

        intMonth++;

        String strDay = "0" + intDay.toString();
        String strMonth = "0" + intMonth.toString();
        String strYear = intYear.toString();
        String strHours = "0" + intHours.toString();
        String strMinutes = "0" + intMinutes.toString();
        String strSeconds = "0" + intSeconds.toString();

        strDateTime = strYear + "-" + strMonth.substring(strMonth.length()-2) + "-" + strDay.substring(strDay.length()-2) + " " +
                strHours.substring(strHours.length()-2) + ":" + strMinutes.substring(strMinutes.length()-2) + ":" + strSeconds.substring(strSeconds.length()-2);

        return strDateTime;

    }

    //
    //
    //
    class AsyncCheckCode extends AsyncTask<String, String, String> {

        // Requiere permisos en el Manifest
        //
        // <uses-permission android:name="android.permission.INTERNET"/>
        // <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
        //
        // y en el tag de <aplicacion>:
        // android:usesCleartextTraffic="true"

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            String resp = "";
            if (isInternetAvailable()) {
                try {
                    URL url = new URL("https://sigmed.minedu.gob.pe/editor/wcf/Service.svc/ValidateCode?CodMod="+params[0]+"&Anexo=0");
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(3000);
                    urlConnection.connect();

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
                                resp = strInputStream.toString();

                                //Borrar esto cuando se actualice el nuevo Servicio Web
                                if (resp.length() > 0) {
                                    resp = resp.replace("\\", "");
                                    resp = resp.substring(1);
                                    resp = resp.substring(0, resp.length() - 1);
                                }
                                //
                            }
                            inputStream.close();
                        }
                    } finally {
                        urlConnection.disconnect();
                    }

                } catch (IOException e) {
                    Log.e("AsyncCheckCode", "Error while open connection to server" + e.getMessage().toString(), e);
                    e.printStackTrace();
                }
            }
            return resp;
        }

        @Override
        protected void onPostExecute(String result) {

            if (result.length() > 0) {

                String strCenEdu = "";
                txtCentroEducativo.setTextColor(getResources().getColor(R.color.secondary_text));
                try {
                    JSONArray jsonArray = new JSONObject(result).getJSONArray("Result");
                    //if (jsonArray != null) {
                    if (jsonArray.length() == 1) {
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        strCenEdu = jsonObject.getString("CEN_EDU");
                        //jsonArray.getString("CEN_EDU");
                        flgCode = "1";
                    } else {
                        strCenEdu = "El código ingresado no es válido";
                        txtCentroEducativo.setTextColor(getResources().getColor(R.color.red));
                        flgCode = "3";
                    }
                    lblBlankSpace.setVisibility(View.GONE);
                    txtCentroEducativo.setVisibility(View.VISIBLE);
                } catch (JSONException ex) {
                    //Toast.makeText(Main.this, "Error en acceder a la respuesta", Toast.LENGTH_SHORT).show();
                    Log.e("AsyncCheckCode", "Error reading response: " + ex.getMessage().toString());
                    //ex.printStackTrace();
                }
                txtCentroEducativo.setText(strCenEdu);
            }

        }

        //Repetido en LogOut
        private boolean isInternetAvailable() {
            if (isNetworkAvailable()) {
                try {
                    HttpURLConnection urlConnection = (HttpURLConnection) (new URL("https://sigmed.minedu.gob.pe/editor/wcf/Service.svc/CheckService?ping=1").openConnection());
                    urlConnection.setConnectTimeout(3000);
                    urlConnection.connect();
                    return (urlConnection.getResponseCode() == 200);
                } catch (SocketTimeoutException e) {
                    Log.e("AsyncCheckCode", "Error timeout: " + e.getMessage().toString(), e);
                } catch (IOException e) {
                    Log.e("AsyncCheckCode", "Error checking service available: " + e.getMessage().toString(), e);
                }
            }
            return false;
        }

        //Repetido en LogOut
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


}