package me.romanow.lep500;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

public class GPSPoint {
    final static int GeoNone=0;
    final static int GeoGPS=1;
    final static int GeoNet=2;
    final static double gradus=111.12;
    private int state= GeoNone;
    private double geoy=0;           // Широта
    private double geox=0;           // Долгота
    private long gpsTime = 0;
    public int state(){ return state; }
    public double geox() {
        return geox;
    }
    public double geoy() {
        return geoy;
    }
    public long geoTime(){ return gpsTime; }
    public void setCurrentTime(){ gpsTime = new Date().getTime(); }
    public GPSPoint(){ state = GeoNone; }
    public GPSPoint(String y0,String x0, boolean exact){
        try {
            geoy=fromStr(y0); geox=fromStr(x0);
            state = exact ? GeoGPS : GeoNet;
            } catch(Throwable ee){ state = GeoNone; }
        }
    public GPSPoint(double y0,double x0, boolean exact){
        geoy=y0; geox=x0;
        state = exact ? GeoGPS : GeoNet;
        }
    public GPSPoint(double y0,double x0, boolean exact,long timeMs){
        geoy=y0; geox=x0;
        state = exact ? GeoGPS : GeoNet;
        gpsTime = timeMs;
        }
    public long elapsedTimeInSec(){ return new DateTime().getMillis()-gpsTime; }
    public GPSPoint(String crd,boolean exact){
        setCoord(crd,exact);
        }
    public void state(int st){
        state = st;
    }
    public boolean gpsValid(){
        return state != GeoNone;
    }
    public void setCoord(double y, double x, boolean exact){
        geox=x;
        geoy=y;
        state = exact ? GeoGPS : GeoNet;
        }
    public GPSPoint  copy(){
        GPSPoint out = new GPSPoint(geoy,geox,true);
        out.state = state;
        out.gpsTime = new DateTime().getMillis();
        return out;
        }
    //----------- Конвертирование в формат ggmm.xxxxx (градусы-минуты-дробная часть)
    public static double fromStr(String ss) throws Throwable{
        double dd=0;
        dd=Double.parseDouble(ss);
        return dd;
        }
    public static String toStr(double dd){
        String ss=""+dd;
        int k=ss.indexOf(".")+5;
        if (k>ss.length()) k=ss.length();
        return ss.substring(0, k);
        }
    public String toStrX(){
        if (state == GeoNone)
            return "";
        return toStr(geox);
        }
    public String toStrY(){
        if (state == GeoNone)
            return "";
        return toStr(geoy);
        }
    public static String convert(double dd){
        String ss=""+(int)dd;
        dd-=(int)dd;
        dd*=60;
        int xx=(int)dd;
        if (xx<10) ss+="0"+xx; else ss+=""+xx;
        String s2=""+dd;
        int k=s2.indexOf(".");
        ss+=s2.substring(k);
        return ss;
        }
    public void copy(GPSPoint src){
        geox=src.geox;
        geoy=src.geoy;
        state = src.state;
        }
    public int diff(GPSPoint T){        // В метрах
        if (state== GeoNone || T.state== GeoNone)
            return -1;
        double dx=(geox-T.geox)*gradus*1000*Math.cos(Math.PI*geoy/180);
        double dy=(geoy-T.geoy)*gradus*1000;
        return (int)Math.sqrt(dy*dy+dx*dx);
        }
    public String toString(){
        if (state== GeoNone)
            return "Нет геоданных";
        DateTimeFormatter dtf = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss");
        return (state==GeoGPS ? "gps " : "net ")+toStr(geoy)+","+toStr(geox)+" "+dtf.print(gpsTime);
        }
    public String toShortString(){
        if (state== GeoNone)
            return "";
        return toStr(geoy)+","+toStr(geox);
        }
    public String getTitle(){
        if (state== GeoNone)
            return "";
        return toStr(geoy)+","+toStr(geox);
    }
    public void moveY(double dd){
        geoy+=dd/1000/gradus;
    }
    public void moveX(double dd){
        geox+=dd/1000/gradus/Math.cos(Math.PI*geoy/180);
    }
    public void setCoord(String crd,boolean exact){
        state = GeoNone;
        int k=crd.indexOf(",");
        if (k==-1) return;
        double v1=0,v2=0;
        try {
            v1=Double.parseDouble(crd.substring(0,k).trim());
            v2=Double.parseDouble(crd.substring(k+1).trim());
            setCoord(v1, v2,exact);
            } catch(Throwable ee){ state= GeoNone; }
        }
    public String toStringValue() {
        return toShortString()+"|"+new DateTime(gpsTime);
        }
    public void parseValue(String ss) throws Exception {
        int idx=ss.indexOf("|");
        if (idx==-1)
            return;
        setCoord(ss.substring(0,idx),true);
        gpsTime = Long.parseLong(ss.substring(idx+1));
    }
}
