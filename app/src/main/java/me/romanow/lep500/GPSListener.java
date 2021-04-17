package me.romanow.lep500;

public interface GPSListener {
    public void onEvent(String ss);
    public void onGPS(GPSPoint gpsPoint);
}
