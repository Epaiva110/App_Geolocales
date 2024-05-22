package com.example.censoeducativo;

public class DataList {

    private String strFecha, strCODMOD,  strCENEDU, strLat, strLng, strPrec, strSend;

    public DataList (String strFecha, String strCODMOD, String strCENEDU, String strLat, String strLng, String strPrec, String strSend){
        this.strFecha = strFecha;
        this.strCODMOD = strCODMOD;
        this.strCENEDU = strCENEDU;
        this.strLat = strLat;
        this.strLng = strLng;
        this.strPrec = strPrec;
        this.strSend = strSend;
    }

    public String getFecha(){
        return strFecha;
    }
    public String getCODMOD(){
        return strCODMOD;
    }
    public String getCENEDU(){
        return strCENEDU;
    }
    public String getLat(){
        return strLat;
    }
    public String getLng(){
        return strLng;
    }
    public String getPrec(){
        return strPrec;
    }
    public String getSend(){
        return strSend;
    }

}
