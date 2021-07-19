package me.romanow.lep500.yandexmap;

import android.os.Bundle;

import java.util.ArrayList;

import me.romanow.lep500.AppData;
import me.romanow.lep500.FileDescription;
import me.romanow.lep500.GPSListener;
import me.romanow.lep500.GPSPoint;
import me.romanow.lep500.R;

public class MapFilesActivity extends MapActivity340{
    public GPSListener gpsBack = new GPSListener() {
        @Override
        public void onEvent(String ss) { }
        @Override
        public void onGPS(GPSPoint gpsPoint) {
            paintSelf();
            }
        };
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppData.ctx().getGpsService().addGPSListener(gpsBack);
        paintSelf();
        moveToSelf();
        ArrayList<FileDescription> list = AppData.ctx().getFileList();
        for(FileDescription file : list)
            paint(file.toString(),file.gps,R.drawable.where,true);
        }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppData.ctx().getGpsService().removeGPSListener(gpsBack);
        }
    }
