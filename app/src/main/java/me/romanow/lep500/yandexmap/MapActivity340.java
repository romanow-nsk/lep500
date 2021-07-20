package me.romanow.lep500.yandexmap;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.ui_view.ViewProvider;

import java.util.StringTokenizer;

import me.romanow.lep500.AppData;
import me.romanow.lep500.BaseActivity;
import me.romanow.lep500.GPSPoint;
import me.romanow.lep500.GPSService;
import me.romanow.lep500.R;
import me.romanow.lep500.fft.FFTStatistic;

/**
 * This is a basic example that displays a map and sets camera focus on the target location.
 * Note: When working on your projects, remember to request the required permissions.
 */
public class MapActivity340 extends BaseActivity {
    protected MapObjectCollection mapObjects;
    protected int wx=800,wy=600;
    protected int zoom=20;
    protected MapView mapView;
    protected Handler animationHandler = new Handler();
    protected PlacemarkMapObject myPlace=null;
    private GPSService gpsService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        MapKitFactory.setApiKey(AppData.MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.map340);
            gpsService = AppData.ctx().getGpsService();
            mapView = (MapView)findViewById(R.id.mapview340);
            // And to show what can be done with it, we move the camera to the center of Saint Petersburg.
            //mapView.getMap().move(new CameraPosition(TARGET_LOCATION, 14.0f, 0.0f, 0.0f), new Animation(Animation.Type.SMOOTH, 5), null);
            wy=this.getWindowManager().getDefaultDisplay().getHeight();
            wx=this.getWindowManager().getDefaultDisplay().getWidth();
            mapObjects = mapView.getMap().getMapObjects().addCollection();
            onMyCreate();
            }
        catch(Exception e1){
            popupInfo(e1.toString()); }
            }
    protected void moveToSelf(){
        moveTo(gpsService.lastGPS());
    }
    protected GPSPoint getDefaultLocation(){
        return gpsService.lastGPS();
        }
    protected void moveTo(GPSPoint pp){
        if (pp==null || !pp.gpsValid())
            return;
        Point point =  new Point(pp.geoy(),pp.geox());
        moveTo(point);
    }
    protected void moveTo(Point point) {
        mapView.getMap().move(new CameraPosition(point, 20.0f, 0.0f, 0.0f), new Animation(Animation.Type.SMOOTH, 5), null);
        }
    @Override
    protected void onStop() {
        // Activity onStop call must be passed to both MapView and MapKit instance.
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
        }
    @Override
    protected void onStart() {
        // Activity onStart call must be passed to both MapView and MapKit instance.
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
        }
    protected void paint(String text,GPSPoint geo,int icon,boolean moveTo){
        paint(text,geo,icon,moveTo,0,null);
        }

    private void popupText(Point point, String text){
        LinearLayout xx=(LinearLayout)getLayoutInflater().inflate(R.layout.listbox_item, null);
        xx.setBackgroundColor(0x00FFFFFF);
        xx.setPadding(5, 5, 5, 5);
        Button tt=(Button) xx.findViewById(R.id.dialog_listbox_name);
        text = text.replace("...","\n");
        StringTokenizer ss = new StringTokenizer(text,"\n");
        int cnt = ss.countTokens();
        tt.setLines(cnt);
        tt.setTextSize(15);
        tt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tt.setText(text);
        //------------------------------------------------------------------------------------------
        //final ViewProvider viewProvider = new ViewProvider(textView);
        final ViewProvider viewProvider = new ViewProvider(xx);
        final PlacemarkMapObject viewPlacemark = mapObjects.addPlacemark(point, viewProvider);
        viewProvider.snapshot();
        viewPlacemark.setView(viewProvider);
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mapObjects.remove(viewPlacemark);
            }
        }, AppData.PopupShortDelay*1000);
    }

    protected void paintSelf(){
        final GPSPoint geo = gpsService.lastGPS();
        if (!geo.gpsValid()) return;
        if (myPlace!=null)
            mapObjects.remove(myPlace);
        myPlace = mapObjects.addPlacemark(new Point(geo.geoy(),geo.geox()));
        myPlace.setOpacity(0.5f);
        myPlace.setIcon(ImageProvider.fromResource(this,R.drawable.user_location_gps));
        myPlace.setDraggable(false);         // ПЕРЕМЕЩЕНИЕ !!!!!!!!!!!!
        myPlace.addTapListener(new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(MapObject mapObject, Point point) {
                String text = geo.toString();
                popupText(point, text);
                return true;
                }
            });
        }
    protected PlacemarkMapObject paint(final String text,GPSPoint geo,int icon,boolean moveTo, final int idx, final I_MapSelect back){
        if (!geo.gpsValid())
            return null;
        Point gp=new Point(geo.geoy(),geo.geox());
        return paint(text,gp,icon,moveTo,idx,back);
        }

    protected PlacemarkMapObject paint(final String text,Point gp,int icon,boolean moveTo, final int idx, final I_MapSelect back){
        final PlacemarkMapObject circle = mapObjects.addPlacemark(gp);
        circle.setOpacity(0.5f);
        circle.setIcon(ImageProvider.fromResource(this,icon));
        circle.setDraggable(moveTo);         // ПЕРЕМЕЩЕНИЕ !!!!!!!!!!!!
        circle.addTapListener(new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(MapObject mapObject, Point point) {
                popupText(point, text);
                if (back!=null)
                    back.onSelect(idx);
                return true;
                }
            });
        if (moveTo) moveTo(gp);
        return circle;
    }
    public void onMyCreate(){}          // Отложенные действия
    //----------------------------------------------------------------------------------
    @Override
    public void clearLog() {}
    @Override
    public void addToLog(String ss, int textSize) {
        popupInfo(ss);
        }
    @Override
    public void addToLogHide(String ss) {
        popupInfo(ss);
        }
    @Override
    public void popupAndLog(String ss) {
        popupInfo(ss);
        }
    @Override
    public void showStatisticFull(FFTStatistic inputStat, int idx) {}
}
