package com.example.censoeducativo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Archive extends AppCompatActivity {

    private ImageView imgClose;
    private RecyclerView rcwSendList;
    private List<DataList> dataList;

    //Temporales
    private Integer intTotalSend, intPendentSend;
    private String strResume;
    private Button btnShow;
    private TextView txtTest, txtResume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Quitamos ActionBar de la parte superior
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();

        // Agregamos layout de la actividad
        setContentView(R.layout.activity_archive);

        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        imgClose = (ImageView) findViewById(R.id.imgClose);
        rcwSendList = (RecyclerView) findViewById(R.id.rcwSendList);

        //Temporales
        btnShow = (Button) findViewById(R.id.btnShow);
        txtTest = (TextView) findViewById(R.id.txtTest);
        txtResume = (TextView) findViewById(R.id.lblResume);

        intTotalSend = 0;
        intPendentSend = 0;
        strResume = "";
        //

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        dataList = new ArrayList<>();

        GeoDatafile geoDatafile = new GeoDatafile();
        String fileContents = geoDatafile.ReadGeoData(this);

        if (fileContents != null){

            try {

                JSONObject jsonObject = new JSONObject(fileContents);
                JSONObject jsonItems = jsonObject.getJSONObject("geodata");
                JSONArray jsonItemNames = jsonItems.names();

                for (int i=jsonItems.length(); i>0; i--){
                //for (int i=0; i<jsonItems.length(); i++){

                    JSONObject obj = jsonItems.getJSONObject(jsonItemNames.getString((i-1)));
                    String strDateTime =obj.getString("DateTime");
                    String strCODMOD =obj.getString("CodMod");
                    String strCENEDU =obj.getString("CenEdu");
                    String strPrecision =obj.getString("Precision");
                    String strSend =obj.getString("Send");

                    DataList listItem = new DataList(
                            strDateTime,strCODMOD, strCENEDU, "0.000", "0.000", strPrecision, strSend
                    );

                    dataList.add(listItem);

                    if (strSend.equals("0")){
                        intPendentSend++;
                    }
                    intTotalSend++;
                }
            } catch (JSONException e) {
                //Toast.makeText(Archive.this, "Error when access geodatafile", Toast.LENGTH_SHORT).show();
                Log.e("Historial", "Error when access geodatafile: " + e.getMessage().toString());
                e.printStackTrace();
            }
        }

        AdapterList adapterList = new AdapterList(dataList);
        rcwSendList.setAdapter(adapterList);
        rcwSendList.setLayoutManager(new LinearLayoutManager(this));

        // Resume message
        /*
        if (intPendentSend > 0){
            strResume = intTotalSend.toString() + " coordenadas registradas, " + intPendentSend.toString() + " pendientes de envío";
        }
        else {
            strResume = intTotalSend.toString() + " coordenadas enviadas.";
        }
        txtResume.setText(strResume);

        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String xxx = getFilesDir().toString();
                String filename = geodataFileName;
                String fileContents = ReadGeoData(geodataFileName);
                txtTest.setText(fileContents);
            }
        });
        */
        //

    }

}