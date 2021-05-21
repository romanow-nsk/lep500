package me.romanow.lep500;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BTViewFace {
    final public MainActivity face;
    public final int SensorMaxNumber=4;
    private BluetoothLeScanner scanner = null;
    private ImageView BTState[]=new ImageView[4];
    private TextView BTStateText[]=new TextView[4];
    private int BTStateInt[]=new int[4];
    private ImageView BTScanerState;
    public final static int BT_Gray=0;
    public final static int BT_Red=1;
    public final static int BT_Yellow=2;
    public final static int BT_Green=3;
    public final static int BT_LightRed=4;
    public final static int BT_LightGreen=5;
    ArrayList<BTReceiver> sensorList = new ArrayList<>();
    private final static int BTStateID[]={R.drawable.status_gray,R.drawable.status_red,R.drawable.status_yellow,
            R.drawable.status_green,R.drawable.status_light_red,R.drawable.status_light_green};
    public BTViewFace(MainActivity face) {
        this.face = face;
        }
    public void init(){
        BTState[0] = (ImageView) face.findViewById(R.id.headerState0);
        BTState[1] = (ImageView) face.findViewById(R.id.headerState1);
        BTState[2] = (ImageView) face.findViewById(R.id.headerState2);
        BTState[3] = (ImageView) face.findViewById(R.id.headerState3);
        BTScanerState  = (ImageView) face.findViewById(R.id.headerScanerState);
        BTScanerState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scannerOnOff();
            }
        });
        BTStateText[0] = (TextView) face.findViewById(R.id.headerStateText0);
        BTStateText[1] = (TextView) face.findViewById(R.id.headerStateText1);
        BTStateText[2] = (TextView) face.findViewById(R.id.headerStateText2);
        BTStateText[3] = (TextView) face.findViewById(R.id.headerStateText3);
        for(int i=0;i<4;i++){
            final int idx=i;
            BTState[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (idx>=SensorMaxNumber)
                        return;
                    new ListBoxDialog(face, MenuItems, getSensorName(sensorList.get(idx)), new I_ListBoxListener() {
                        @Override
                        public void onSelect(int index) {
                            procMenuItem(sensorList.get(idx),index);
                            }
                        @Override
                        public void onLongSelect(int index) {}
                        }).create();
                    }
                });
            }
        }
    private void initView(){
        for(int i=0;i<4;i++){
            BTStateInt[i]=BT_Gray;
            BTStateText[i].setText("");
            BTState[i].setImageResource(BTStateID[BT_Gray]);
            }
        }
    private void setCurrentView(){
        for(int i=0;i<4;i++){
            if (i<sensorList.size()){
                BTStateText[i].setText("");
                BTState[i].setImageResource(BTStateID[BT_Gray]);
            }
            else{
                BTStateText[i].setText(getSensorName(sensorList.get(i)));
                BTState[i].setImageResource(BTStateInt[i]);
            }
        }
    }
    private void setBTScanerState(int state){
        BTScanerState.setImageResource(BTStateID[state]);
    }
    private void setBTPopup(BTReceiver receiver,String text){
        int idx = sensorList.indexOf(receiver);
        if (idx!=-1 && idx < SensorMaxNumber)
            face.popupInfo(getSensorName(receiver)+":"+text);
        }
    private void setBTName(final BTReceiver receiver){
        face.guiCall(new Runnable() {
            @Override
            public void run() {
                int idx = sensorList.indexOf(receiver);
                if (idx!=-1 && idx < SensorMaxNumber){
                    BTStateText[idx].setText(getSensorName(true,receiver));
                }
            }
        });
    }
    private void setBTState(final BTReceiver receiver,final int state){
        face.guiCall(new Runnable() {
            @Override
            public void run() {
                int idx = sensorList.indexOf(receiver);
                if (idx!=-1 && idx < SensorMaxNumber){
                    BTStateInt[idx]=state;
                    BTState[idx].setImageResource(BTStateID[state]);
                }
            }
        });
    }
    private void setBTState(final BTReceiver receiver){
        face.guiCall(new Runnable() {
            @Override
            public void run() {
                int idx = sensorList.indexOf(receiver);
                if (idx!=-1 && idx < SensorMaxNumber){
                    BTState[idx].setImageResource(BTStateID[idx]);
                    BTStateText[idx].setText(getSensorName(true,receiver));
                }
            }
        });
    }
    private void setBTStateText(final BTReceiver receiver,final String text){
        face.guiCall(new Runnable() {
            @Override
            public void run() {
                int idx = sensorList.indexOf(receiver);
                if (idx!=-1 && idx < SensorMaxNumber){
                    BTStateText[idx].setText(text);
                }
            }
        });
    }

    private boolean scannerOn=false;
    public void scannerOnOff(){
        if (!scannerOn)
            blueToothOn();
        else
            scannerOff();
        }
    public void blueToothOn(){
        blueToothOff();
        BluetoothAdapter bluetooth= BluetoothAdapter.getDefaultAdapter();
        if(bluetooth==null) {
            face.addToLog("Нет модуля BlueTooth");
            setBTScanerState(BT_Red);
            return;
        }
        if (!bluetooth.isEnabled()) {
            // Bluetooth выключен. Предложим пользователю включить его.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            face.startActivityForResult(enableBtIntent, face.REQUEST_ENABLE_BT);
            return;
        }
        bluetooth.setName(face.BT_OWN_NAME);
        int btState= bluetooth.getState();
        if (btState==BluetoothAdapter.STATE_ON){
            face.addToLog("Состояние BlueTooth: включен");
            setBTScanerState(BT_Green);
            }
        if (btState==BluetoothAdapter.STATE_TURNING_ON){
            face.addToLog("Состояние BlueTooth: включается");
            setBTScanerState(BT_Yellow);
            }
        if (btState==BluetoothAdapter.STATE_OFF){
            face.addToLog("Состояние BlueTooth: выключен");
            setBTScanerState(BT_Red);
            }
        if (btState==BluetoothAdapter.STATE_TURNING_OFF){
            face.addToLog("Состояние BlueTooth: выключается");
            setBTScanerState(BT_Yellow);
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();
        if (scanner != null) {
            sensorList.clear();
            List<ScanFilter> filters = new ArrayList<>();
            int filterMode=0;
            // ----------------- Сканирование по UUID
            if (filterMode==1){
                UUID BLP_SERVICE_UUID = UUID.fromString(BTReceiver.UUID_SERVICE_STR);
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(BLP_SERVICE_UUID)).build();
                filters.add(filter);
            }
            // --------------- Сканирование по имени
            if (filterMode==2) {
                String[] names = new String[]{face.BT_SENSOR_NAME_PREFIX};
                for (String name : names) {
                    ScanFilter filter = new ScanFilter.Builder().setDeviceName(name).build();
                    filters.add(filter);
                }
            }
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();
            scanner.startScan(filters, scanSettings, BTScanCallback);
            initView();
            face.popupAndLog("Сканирование началось");
            scannerOn=true;
            scannerHandler.postDelayed(scanerTimeOut,face.BT_SCANNING_TIME_IN_SEC*1000);
            }
        else{
            face.popupAndLog("Сканер BleuTooth не получен");
            setBTScanerState(BT_Red);
            return;
            }
        }
    public void blueToothOff(){
        for(BTReceiver receiver : sensorList)
            receiver.blueToothOff();
        sensorList.clear();
        setBTScanerState(BT_Gray);
        }
    public void startScanner(){
        if (scanner!=null)
            scanner.stopScan(BTScanCallback);
        setBTScanerState(BT_Gray);
        }
    //----------------------------------------------------------------------------------------------
    public void selectSensor(final SensorListener listener){
        if (sensorList.size()==0){
            face.popupAndLog("Нет включенных датчиков");
            return;
            }
        ArrayList<String> sensorNames = new ArrayList<>();
        for(BTReceiver receiver : sensorList)
            sensorNames.add(getSensorName(receiver));
        new ListBoxDialog(face, sensorNames, "Датчик", new I_ListBoxListener() {
            @Override
            public void onSelect(int index) {
                listener.onSensor(sensorList.get(index));
            }
            @Override
            public void onLongSelect(int index) { }
                }
            ).create();
        }
    public void selectSensorGroup(final SensorGroupListener listener){
        if (sensorList.size()==0){
            face.popupAndLog("Нет включенных датчиков");
            return;
        }
        ArrayList<String> sensorNames = new ArrayList<>();
        for(BTReceiver receiver : sensorList)
            sensorNames.add(getSensorName(receiver));
        new MultiListBoxDialog(face,  "Датчики (старт)", sensorNames, new MultiListBoxListener() {
            @Override
            public void onSelect(boolean[] selected) {
                ArrayList<BTReceiver> out = new ArrayList<>();
                for(int i=0;i<selected.length;i++)
                    if (selected[i])
                        out.add(sensorList.get(i));
                listener.onSensor(out);
                }
            });
        }
    //----------------------------------------------------------------------------------------------
    private void scannerOff(){
        if (scanner!=null)
            scanner.stopScan(BTScanCallback);
        setBTScanerState(BT_Gray);
        scannerOn=false;
        }
    Handler scannerHandler = new Handler();
    Runnable scanerTimeOut = new Runnable() {
        @Override
        public void run() {
            face.addToLog("Тайм-аут сканирования");
            scannerOff();
            }
        };
    public String getSensorName(BTReceiver receiver){
        return getSensorName(false,receiver);
        }
    public String getSensorName(boolean shortName,BTReceiver receiver){
        BTDescriptor descriptor = face.set.addressMap.get(receiver.getSensorMAC());
        return descriptor==null ? receiver.getSensorName()+" "+(shortName ? "" : receiver.getSensorMAC()) : descriptor.btName;
        }
    private boolean isMACAddressPresent(String ss){
        for(BTReceiver receiver : sensorList)
            if (receiver.getSensorMAC().equals(ss))
                return true;
        return false;
        }
    //----------------------------------------------------------------------------------------------
    private final ScanCallback BTScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName()==null)
                return;
            face.addToLog(true,"BlueTooth: "+device.getName()+" "+device.getAddress());
            if (isMACAddressPresent(device.getAddress())){
                face.addToLog(true,"повторное сканирование BlueTooth: "+device.getAddress());
                return;
            }
            if (device.getName().startsWith(face.BT_SENSOR_NAME_PREFIX)){
                face.addToLog(true,"BlueTooth: "+device.getName()+" подключение");
                BTReceiver receiver = new BTReceiver(BTViewFace.this,BTBack);
                receiver.blueToothOn(device);
                sensorList.add(receiver);
                }
            }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {}
        @Override
        public void onScanFailed(int errorCode) {}
        };
    //---------------------------------------------------------------------------------------------------------
    public BTListener BTBack = new BTListener() {
        @Override
        public void notify(BTReceiver sensor, boolean fullInfoMes, String ss) {
            face.addToLog(fullInfoMes,getSensorName(sensor)+": "+ss);
            }
        @Override
        public void onReceive(BTReceiver sensor, LEP500File file){
            face.saveBTFile(sensor,file);
            setBTState(sensor,BT_Green);
            setBTName(sensor);
            }
        @Override
        public void onState(BTReceiver sensor, int state) {
            setBTState(sensor,state);
            }
        @Override
        public void onStateText(BTReceiver sensor, String text) {
            setBTStateText(sensor,text);
            }
        @Override
        public void onPopup(BTReceiver sensor, String text) {
            setBTPopup(sensor,text);
            }
    };
    //------------------------------------------------------------------------------------------
    private String[] MenuItems = {
        "Выключить",
        "Измерение",
        "Имя датчика",
        "Уровень заряда",
        "Прервать"
        };

    public void procMenuItem(final BTReceiver receiver, final int index) {
        switch (index) {
    case 0:
            receiver.deviceOff();
            receiver.blueToothOff();
            break;
    case 1:
            String name = getSensorName(receiver).replace("_","-");
            LEP500File file = new LEP500File(face.set,name,face.gpsService.lastGPS());
            receiver.startMeasure(file,false);
            break;
    case 2:
            final BTDescriptor descriptor = face.set.addressMap.get(receiver.getSensorMAC());
            new OneParameterDialog(face, "Имя датчика",receiver.getSensorMAC(),descriptor==null ? "" : descriptor.btName, false, true,new I_EventListener() {
                @Override
                public void onEvent(String ss) {
                    if (descriptor!=null)
                        descriptor.btName = ss;
                    else
                        face.set.knownSensors.add(new BTDescriptor(ss,receiver.getSensorMAC()));
                    face.set.createMaps();
                    face.saveSettings();
                    BTStateText[index].setText(getSensorName(receiver));
                    }
                });
            break;
    case 3:
            receiver.getChargeLevel();
            break;
    case 4:
           receiver.stopMeasure();
           break;
           }
        }
    }
