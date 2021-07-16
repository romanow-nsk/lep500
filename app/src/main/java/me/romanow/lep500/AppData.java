package me.romanow.lep500;

import android.app.Application;

import com.jjoe64.graphview.LineGraphView;

import java.util.ArrayList;

public class AppData extends Application {
    public final static String apkVersion = "1.0.2, 16.07.2021";
    private static AppData ctx = null;
    private LEP500Settings settings = new LEP500Settings();
    private FileDescriptionList fileList = new FileDescriptionList();
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
}
