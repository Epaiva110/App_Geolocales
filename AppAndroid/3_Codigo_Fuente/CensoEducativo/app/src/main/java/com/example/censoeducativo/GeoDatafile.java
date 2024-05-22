package com.example.censoeducativo;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

public class GeoDatafile {

    private String geodataFileName = "geodata.txt";

    private String initialFileContent = "{\n" +
            "\"geodata\": {\n"+
            "}\n" +
            "}";

    public void CreateDatafile(Context context){

        File file = new File(context.getFilesDir(),geodataFileName);

        if (!file.exists()){
            try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(geodataFileName, context.MODE_PRIVATE))) {
                outputStreamWriter.write(initialFileContent);
            } catch (IOException e) {
                Log.e("CreateDataFile", "Error creating geodatafile" + e.getMessage().toString(), e);
                e.printStackTrace();
            }
        }
    }

    public String ReadGeoData(Context context) {

        String geodataFileContent = null;

        try {
            InputStream isFile = context.openFileInput(geodataFileName);

            Integer intSize = isFile.available();
            byte[] bteBuffer= new byte[intSize];
            isFile.read(bteBuffer);
            isFile.close();
            geodataFileContent = new String(bteBuffer,"UTF-8");

        } catch (IOException e) {
            Log.e("ReadGeoData", "Error when reading geodatafile" + e.getMessage().toString(), e);
            e.printStackTrace();
        }

        return geodataFileContent;
    }

    public String UpdateDatafile(Context context, String dataString) {

        String strError = "0";

        if (!dataString.equals("")) {
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(geodataFileName, context.MODE_PRIVATE));
                outputStreamWriter.append(dataString);
                //outputStreamWriter.write(fileContents);
                outputStreamWriter.close();
            } catch (IOException e) {
                strError = "Error al momento de actualizar el archivo de datos";
                Log.e("UpdateDatafile", "Error when update geodatafile: " + e.getMessage().toString());
                e.printStackTrace();
            }
        }

        return strError;
    }
}
