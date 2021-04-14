package me.romanow.lep500;

import org.joda.time.DateTime;

import romanow.snn_simulator.fft.FFTAudioTextFile;

public class LEP500File extends FFTAudioTextFile {
    DateTime createDate = new DateTime();
    String postNumber="";           // Номер опоры
    String sensorNumber="";         // Номер датчика
    String comment="";              // Комментарий
    String originalFileName="";     // Оригинальное имя
    GPSPoint gps = new GPSPoint();
    public void createData(int sz0){
        sz = sz0;
        data = new float[sz];
        }
    public float []getData(){  return data; }
}
