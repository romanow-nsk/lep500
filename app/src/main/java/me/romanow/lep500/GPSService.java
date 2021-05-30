package me.romanow.lep500;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.core.app.ActivityCompat;

import org.joda.time.DateTime;

import java.util.Iterator;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public class GPSService {
    public final static int GPSInterval = 10;                 // Интервал опроса GPS-координат (сек)
    public final static int GPSDistance = 20;                 // Интервал изменения координат (м)
    public final static int GPSValidDelay = 10;               // Интервал валидности GPS (мин)
    private boolean gpsOn = false;
    private Handler event = new Handler();
    private LocationManager mLocationManager = null;
    private GPSListener back;
    private GPSPoint lastGPSGeo = new GPSPoint();
    private GPSPoint lastGPSNet = new GPSPoint();
    private Context context;
    private int satCount=0;
    private DateTime lastGPSTime = new DateTime();

    public GPSService(GPSListener lsn) {
        back = lsn;
        }

    public void setDelay(int sec, Runnable code) {
        event.postDelayed(code, sec * 1000);
    }

    public void cancelDelay(Runnable code) {
        event.removeCallbacks(code);
    }

    public void startService(Context context0) {
        context = context0;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mLocationManager == null) {
            back.onEvent("Недоступен менеджер местоположения/навигации");
        } else {
            try {
                if (checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    back.onEvent("Установите разрешения GPS");
                    return;
                    }
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPSInterval * 1000, GPSDistance, locationListener);
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, GPSInterval * 1000, GPSDistance, locationListener);
                setDelay(GPSInterval, gpsClock);
                gpsOn = true;
            } catch (Exception ee) {
                back.onEvent("Ошибка GPS-сервиса: " + ee.toString());
            }
        }
    }

    public void stopService() {
        gpsOn = false;
        cancelDelay(gpsClock);
        if (mLocationManager != null)
            mLocationManager.removeUpdates(locationListener);
        mLocationManager = null;
    }

    public GPSPoint lastGPS() {
        GPSPoint notValid = new GPSPoint();
        if (!lastGPSGeo.gpsValid() && !lastGPSNet.gpsValid())
            return notValid;
        long delay = lastGPSGeo.elapsedTimeInSec()/60;
        if (lastGPSGeo.gpsValid() && delay  <GPSValidDelay)
            return lastGPSGeo;
        return lastGPSNet;          //38 - если нет от сети
    }

    Runnable gpsClock = new Runnable() {
        public void run() {
            try {
                if (mLocationManager == null) {
                    setDelay(GPSInterval, gpsClock);
                    return;
                    }
                Location cL;
                if (checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    back.onEvent("Установите разрешения GPS");
                    return;
                }
                GpsStatus status = mLocationManager.getGpsStatus(null);
                Iterable<GpsSatellite> satellites = status.getSatellites();
                Iterator<GpsSatellite> satI = satellites.iterator();
                while (satI.hasNext()) {
                    GpsSatellite satellite = satI.next();
                    float ff = satellite.getSnr();
                    satCount++;
                    }
                if (checkSelfPermission(context,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(context,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    back.onEvent("Установите разрешения GPS");
                    return;
                    }
                GPSPoint old = lastGPS();
                cL = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (cL != null){
                    lastGPSGeo = new GPSPoint(cL.getLatitude(),cL.getLongitude(),true,cL.getTime());
                    lastGPSTime = new DateTime();
                    }
                else
                    lastGPSGeo = new GPSPoint();
                cL= mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (cL==null)
                    lastGPSNet = new GPSPoint();
                else{
                    lastGPSNet = new GPSPoint(cL.getLatitude(),cL.getLongitude(),false,cL.getTime());
                    lastGPSTime = new DateTime();
                    }
                if (lastGPS().state()!=old.state())
                    back.onGPS(lastGPS());
                if (gpsOn)
                    setDelay(GPSInterval,gpsClock);
                    }
            catch(Throwable ee){
                back.onEvent("Ошибка GPS-сервиса: "+ee.toString());
                stopService();
            }
        }};
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            int v=0;
            }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            int v=0;
        }
        @Override
        public void onProviderEnabled(String provider) {
            int v=0;
        }
        @Override
        public void onProviderDisabled(String provider) {
           int v=0;
        }
    };

}
