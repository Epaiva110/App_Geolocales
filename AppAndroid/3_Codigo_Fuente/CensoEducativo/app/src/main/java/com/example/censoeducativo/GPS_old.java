package com.example.censoeducativo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;


public class GPS_old extends AppCompatActivity {

    public ImageView imgClose;
    public ProgressBar pbrWaitButton;
    private TextView lblGPS;
    private TextView txtLatitud, txtLongitud, txtPrecision, txtAltitud;
    private Boolean flgInitialTime = true, flgInitialStatus = true;
    private int intActionButton = 0;

    private LocationManager locManager;
    private long lngWaitLocationTime = 300000;       // 5 minutos (2 rondas de 2.5 m.)
    private long lngWaitPrecisionTime = 150;         // 2.5 minutos

    // Variables temporlaes
    private TextView lblTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos ActionBar de la parte superior
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        // Agregamos layout de la actividad
        setContentView(R.layout.activity_gps_old);

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        imgClose = (ImageView) findViewById(R.id.imgClose);
        pbrWaitButton = (ProgressBar) findViewById(R.id.pbrWaitButton);
        lblGPS = (TextView) findViewById(R.id.lblWaitButton);
        txtLatitud = (TextView) findViewById(R.id.txtLatitud);
        txtLongitud = (TextView) findViewById(R.id.txtLongitud);
        txtPrecision = (TextView) findViewById(R.id.txtPrecision);
        txtAltitud = (TextView) findViewById(R.id.txtAltitud);

        lblTimer = (TextView) findViewById(R.id.lblTimer);

        // 1. Verificar GPS activado
        if (isLocationEnabled()) {

            // 2. Obtenemos las coordenadas del GPS
            runInitialTime();
            getCoords();

        } else {
            // 3.
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setCancelable(false);
            alertDialog.setTitle(R.string.configuration_message_title)
                    .setMessage(R.string.configuration_message)
                    .setPositiveButton(R.string.configuration_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                            // Llama a la ventana de configuración
                            Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(myIntent);
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                            // Cierra la ventana actual
                            Intent resultIntent = new Intent();
                            setResult(RESULT_CANCELED, resultIntent);
                            finish();
                        }
                    });
            alertDialog.show();
        }

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent resultIntent = new Intent();
                setResult(RESULT_CANCELED, resultIntent);
                finish();

            }
        });

    }

    private boolean isLocationEnabled() {
        return locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // 2.1 Contador de tiempo inicial de espera
    private void runInitialTime() {

        new CountDownTimer(lngWaitLocationTime, 1000) {
            public void onTick(long millisUntilFinished) {
                if ((millisUntilFinished/1000) == lngWaitPrecisionTime){
                    flgInitialTime = false;
                    if (flgInitialStatus== true)
                        lblGPS.setText(R.string.gps_wait);
                }
                lblTimer.setText((lngWaitLocationTime - millisUntilFinished)/1000 + " s.");
            }
            public void onFinish() {
                if (flgInitialStatus) {
                    Rect bounds = pbrWaitButton.getIndeterminateDrawable().getBounds();
                    pbrWaitButton.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_background2));
                    pbrWaitButton.getIndeterminateDrawable().setBounds(bounds);
                    lblGPS.setText(R.string.gps_inactive);

                    locManager.removeUpdates(objLocationListener);
                }
                lblTimer.setText("fin");
            }
        }.start();

    }

    // 2.2 Inicialización del listener para las coordenadas del GPS
    public void getCoords() {

        try {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Sin permisos", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(GPS_old.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            } else {
                //Toast.makeText(this, "Inicia GPS", Toast.LENGTH_LONG).show();
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, objLocationListener);
                //Toast.makeText(this, "Termina GPS", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex){
            Log.e("GPSError", "<<<<< Error del GPS >>>>" + ex.getMessage().toString(), ex);
            Toast.makeText(this, "<<<<< Error del GPS >>>>" + ex.getMessage().toString(), Toast.LENGTH_LONG).show();
        }

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Inicia GPS 2", Toast.LENGTH_LONG).show();
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, objLocationListener);
            } else {
                Toast.makeText(this, "Permiso denegado ...", Toast.LENGTH_LONG).show();
            }
        }

    }

    private final LocationListener objLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {

            if(location != null){

                double gpsLatitud = location.getLatitude();
                double gpsLongitud = location.getLongitude();
                double gpsAltitud = location.getAltitude();
                float gpsAccuracy = location.getAccuracy();

                setButtonAction(Math.round(gpsAccuracy));

                txtLatitud.setText(String.format("%.6f",gpsLatitud));
                txtLongitud.setText(String.format("%.6f",gpsLongitud));
                txtPrecision.setText(String.format("%.6f",gpsAccuracy));
                txtAltitud.setText(String.format("%.6f",gpsAltitud));

                flgInitialStatus = false;

            }
            else{
                Log.e("GPSError", "<<<<< Error del GPS null >>>>");
                Toast.makeText(GPS_old.this, "Error de null", Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

    };

    private void setButtonAction(int Accuracy){

        int intAction = 0;

        if (flgInitialStatus) {
            lblGPS.setText(R.string.gps_accuracy);
        }

        if (flgInitialTime){

            //Dentro del tiempo del contador
            if (Accuracy < 12 )
                intAction = 1;                      //Green
        } else {

            //Fuera del tiempo del contador
            if (Accuracy < 100) {
                if (Accuracy < 51) {
                    if (Accuracy < 12) {
                        intAction = 1;              //Green
                    } else {
                        intAction = 2;              //Yellow
                    }
                } else {
                    intAction = 3;                  // Red
                }
            } else {
                intAction = 0;
            }
        }

        if (intActionButton != intAction){
            Rect bounds = null;

            if (intActionButton==0 || intAction==0){
                activateButton(intActionButton);
            }

            switch(intAction){
                case 1:
                    bounds = pbrWaitButton.getIndeterminateDrawable().getBounds();
                    pbrWaitButton.setIndeterminateDrawable(getResources().getDrawable(R.drawable.gps_background_green));
                    pbrWaitButton.getIndeterminateDrawable().setBounds(bounds);
                    break;
                case 2:
                    bounds = pbrWaitButton.getIndeterminateDrawable().getBounds();
                    pbrWaitButton.setIndeterminateDrawable(getResources().getDrawable(R.drawable.gps_background_yellow));
                    pbrWaitButton.getIndeterminateDrawable().setBounds(bounds);
                    break;
                case 3:
                    bounds = pbrWaitButton.getIndeterminateDrawable().getBounds();
                    pbrWaitButton.setIndeterminateDrawable(getResources().getDrawable(R.drawable.gps_background_red));
                    pbrWaitButton.getIndeterminateDrawable().setBounds(bounds);
                    break;
                case 0:
                    bounds = pbrWaitButton.getIndeterminateDrawable().getBounds();
                    pbrWaitButton.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progress_background));
                    pbrWaitButton.getIndeterminateDrawable().setBounds(bounds);
                    break;
            }
            intActionButton = intAction;
        }

    }

    private void activateButton(int statusSwitch){

        if (statusSwitch == 0) {
            pbrWaitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("gpsLatitud", txtLatitud.getText().toString());
                    resultIntent.putExtra("gpsLongitud", txtLongitud.getText().toString());
                    resultIntent.putExtra("gpsPrecision", txtPrecision.getText().toString());
                    resultIntent.putExtra("gpsAltitud", txtAltitud.getText().toString());
                    setResult(RESULT_OK, resultIntent);
                    finish();

                }
            });
            lblGPS.setText(R.string.gps_activate);
            lblGPS.setTextColor(getResources().getColor(R.color.white));
        } else {
            pbrWaitButton.setOnClickListener(null);
            lblGPS.setText(R.string.gps_accuracy);
            lblGPS.setTextColor(getResources().getColor(R.color.secondary_text));
        }

    }

}