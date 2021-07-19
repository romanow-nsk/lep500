package me.romanow.lep500;

import android.app.Application;

import com.jjoe64.graphview.LineGraphView;

import java.util.ArrayList;

public class AppData extends Application {
    public final static String apkVersion = "1.0.4, 20.07.2021";
    public final static String MAPKIT_API_KEY = "fda3e521-bbc6-4c75-9ec7-ccd4fdaa34d3";
    public final static int PopupShortDelay=4;              // Время короткого popup
    public final static int PopupMiddleDelay=7;             // Время длинного popup
    public final static int PopupLongDelay=10;              // Время длинного popup
    private static AppData ctx = null;
    private LEP500Settings settings = new LEP500Settings();
    private FileDescriptionList fileList = new FileDescriptionList();
    private GPSService gpsService = new GPSService();
    public final static ArrayList<String> winFuncList=new ArrayList<>();{
        winFuncList.add("Прямоугольник");
        winFuncList.add("Треугольник");
        winFuncList.add("Синус");
        winFuncList.add("Парабола");
        }
    private AppData(){}
    public static AppData ctx(){
        if (ctx==null)
            ctx = new AppData();
        return ctx;
        }
    public LEP500Settings set() {
        return settings; }
    public void set(LEP500Settings set) {
        this.settings = set;
        }
    public FileDescriptionList getFileList() {
        return fileList; }
    public void setFileList(FileDescriptionList fileList) {
        this.fileList = fileList; }
    public GPSService getGpsService() {
        return gpsService; }
}
