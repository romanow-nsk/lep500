package me.romanow.lep500;

import android.app.Application;

import com.jjoe64.graphview.LineGraphView;

public class AppData extends Application {
    public final static String apkVersion = "1.0.1, 06.07.2021";
    private static AppData ctx = null;
    private LEP500Settings settings = new LEP500Settings();
    private FileDescriptionList fileList = new FileDescriptionList();
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
