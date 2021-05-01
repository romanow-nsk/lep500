package me.romanow.lep500;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class BTReceiver{
    private final static byte UUID_SERVICE[]={0x00,0x00,(byte)0xfe,0x40,(byte)0xcc,0x7a,0x48,0x2a,
            (byte)0x98,0x4a,0x7f,0x2e,(byte)0xd5,(byte)0xb3,(byte)0xe5,(byte)0x8f};
    private final static byte UUID_WRITE[]={0x00,0x00,(byte)0xfe,0x41,(byte)0x8e,0x22,0x45,0x41,
            (byte)0x9d,0x4c,0x21,(byte)0xed,(byte)0xae,(byte)0x82,(byte)0xed,0x19};
    private final static byte UUID_READ[]={0x00,0x00,(byte)0xfe,0x42,(byte)0x8e,0x22,0x45,0x41,
            (byte)0x9d,0x4c,0x21,(byte)0xed,(byte)0xae,(byte)0x82,(byte)0xed,0x19};
    public final static String UUID_SERVICE_STR = "0000fe40-cc7a-482a-984a-7f2ed5b3e58f";
    private final static String UUID_WRITE_STR = "0000fe41-8e22-4541-9d4c-21edae82ed19";
    private final static String UUID_READ_STR = "0000fe42-8e22-4541-9d4c-21edae82ed19";
    private final int SENSOR_CMD_START=1;           // Команда - начать измерения
    private final int SENSOR_CMD_STOP=2;            // Команда - прервать измерения
    private final int SENSOR_CMD_CHARGE_LEVEL=6;    // Команда - получить заряд батереи
    private final int SENSOR_CMD_OFF=8;             // Команда - отключить сенсор
    private final int SENSOR_ANS_DATA=3;            // Ответ - блок данных, параметр = смещение
    private final int SENSOR_ANS_STOP=4;            // Ответ - завершение измерения, параметр 0 - прервано, иначе- кол-во байтов
    private final int SENSOR_ANS_CHARGE_LEVEL=7;    // Ответ - параметр = заряд батереи
    private final int SENSOR_ANS_ERROR=5;           // Ответ - параметр = код ошибки
    private final int WAIT_FOR_READ=10;             // Цикл засыпания при отсутствии данных
    //==============================================================================================
    private boolean BLEisOn=false;
    private LEP500File file;
    private BTListener back;
    private BluetoothDevice device;
    private Activity activity;
    private short buffer[] = new short[10];
    private short data[]=null;
    private BluetoothGatt gatt=null;
    private BluetoothGattService rwService=null;
    private String sensorName="";
    private String sensorMAC="";
    //----------------------------------------------------------------------------------------------
    public String getSensorName() { return sensorName; }
    public String getSensorMAC() { return sensorMAC; }
    public synchronized boolean isWorking() {
        return data != null;
        }
    private void btClose(){
        if (gatt!=null){
            gatt.disconnect();      // Дальше - по событию disconnect
            }
        }
    public BTReceiver(Activity activity0, BTListener back0) {
        back = back0;
        activity = activity0;
        //notify("UUID сенсора");
        //notifyUUID(UUID.nameUUIDFromBytes(UUID_SERVICE));
        //notifyUUID(UUID.nameUUIDFromBytes(UUID_READ));
        //notifyUUID(UUID.nameUUIDFromBytes(UUID_WRITE));
        }
    private void startNotify(){
        if (gatt==null){
            notify(false,"cервис не подключен");
            return;
            }
        if (rwService==null){
            notify(false,"сервис не найден");
            return;
            }
        byte bb[] = new byte[20];
        BluetoothGattCharacteristic received = rwService.getCharacteristic(UUID.fromString(UUID_READ_STR));
        received.setValue(bb);
        gatt.setCharacteristicNotification(received, true);
        for (BluetoothGattDescriptor descriptor : received.getDescriptors()) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            }
        notify(true,"cтарт уведомлений");
        }
    private void receiveAnswer(){
        if (gatt==null){
            notify(false,"сервис не подключен");
            return;
            }
        if (rwService==null){
            notify(false,"сервис не найден");
            return;
            }
        byte bb[] = new byte[20];
        BluetoothGattCharacteristic received = rwService.getCharacteristic(UUID.fromString(UUID_READ_STR));
        received.setValue(bb);
        gatt.readCharacteristic(received);
        notify(false,"старт приема");
        }
    public void sendCommand(int cmd, int param){
        if (gatt==null){
            notify(false,"cервис не подключен");
            return;
            }
        byte bb[] = new byte[4];
        bb[0]=(byte)cmd;
        bb[1]=(byte)(cmd>>8);
        bb[2]=(byte)param;
        bb[3]=(byte)(param>>8);
        BluetoothGattCharacteristic sended = rwService.getCharacteristic(UUID.fromString(UUID_WRITE_STR));
        sended.setValue(bb);
        sended.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        gatt.writeCharacteristic(sended);
        notify(true,"передано "+cmd+" "+param);
        back.onState(this, BTViewFace.BT_Yellow);
        }
    public synchronized void startMeasure(LEP500File file0,boolean tested) {
        startMeasure(file0,-1,tested);
        }
    public synchronized void getChargeLevel()  {
        sendCommand(SENSOR_CMD_CHARGE_LEVEL,0);
        }
    public synchronized void startMeasure(LEP500File file0, int duration,boolean tested) {
        if (isWorking()){
            notify(false,"измерение уже выполняется");
            return;
            }
        file = file0;
        if (duration==-1){
            duration = file.getSettings().measureDuration;
            }
        duration *=100;
        duration -= duration%8;             // Кратность 8;
        data = new short[duration];
        //-------------------------- Тестирование
        file.setData(data);
        if (tested) {
            file.createTestMeasure();
            data = null;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    back.onReceive(BTReceiver.this, file);
                    }
                });
            }
        else{
            sendCommand(SENSOR_CMD_START,duration);
            receiveAnswer();
            }
        }
    public void deviceOff(){
        data = null;
        sendCommand(SENSOR_CMD_OFF,0);
        }
    public void stopMeasure(){
        if (!isWorking()){
            notify(true,"измерение отменено");
            return;
            }
        data = null;
        sendCommand(SENSOR_CMD_STOP,0);
        }
    private void notify(boolean fullInfo, final String mes){
        //activity.runOnUiThread(new Runnable() {
        //    @Override
        //    public void run() {
                back.notify(BTReceiver.this,fullInfo,mes);
        //            }
        //    });
        }
    private String errorCodes[]={"","Недопустимый интервал измерения","Отмена при отсутствии измерений","Неизвестная команда"};
    private String stateCodes[]={"отключено","включается","включено","выключается"};
    private BluetoothGattCallback gattBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            BTReceiver.this.notify(true,"устройство  "+stateCodes[newState]);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    int bondstate = device.getBondState();
                    // Обрабатываем bondState
                    if(bondstate == BOND_NONE || bondstate == BOND_BONDED) {
                        gatt.discoverServices();
                        }
                    else{
                        BTReceiver.this.notify(false,"ошибка подключения status="+status);
                        btClose();
                        back.onState(BTReceiver.this,BTViewFace.BT_Red);
                    }
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    gatt.close();           // Дождаться события от disconnect !!!!!!!!!
                    gatt=null;
                    back.onState(BTReceiver.this,BTViewFace.BT_Gray);
                    break;
                default:
                }
            }
        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            BTReceiver.this.notify(true,"вывод завершен");
            back.onState(BTReceiver.this,BTViewFace.BT_Green);
            }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            BTReceiver.this.notify(true,"ищем сервис "+UUID_SERVICE_STR);
            for (BluetoothGattService service : services) {
                BTReceiver.this.notify(true,"cервис " + service.getUuid().toString() + " " + service.getType());
                if (service.getUuid().toString().equals(UUID_SERVICE_STR)) {
                    rwService = service;
                    BTReceiver.this.notify(true,"Найден сервис " + service.getUuid().toString() + " " + service.getType());
                    //receiveAnswer();
                    startNotify();
                    back.onState(BTReceiver.this,BTViewFace.BT_Green);
                    }
                }
            if (rwService==null)
                BTReceiver.this.notify(false,"Cервис не найден: "+UUID_SERVICE_STR+"\nдатчик "+getSensorMAC());
            }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte bb[] = characteristic.getValue();
            characteristic.setValue(new byte[20]);
            //BTReceiver.this.notify("Принято(2): "+bb.length+" "+characteristic.getStringValue(0)+"\n"+characteristic.getUuid().toString());
            back.onState(BTReceiver.this,BTViewFace.BT_Green);
            procReceived(bb);
            }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic  characteristic, int status) {
            byte bb[] = characteristic.getValue();
            characteristic.setValue(new byte[20]);
            procReceived(bb);
            back.onState(BTReceiver.this,BTViewFace.BT_Green);
            }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            BTReceiver.this.notify(true, "Передано status="+status);
            back.onState(BTReceiver.this,BTViewFace.BT_Green);
            }
        //------------------------------------------------------- прочее -----------------------------------
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            BTReceiver.this.notify(true,"onPhyUpdate "+status);
            }
        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            BTReceiver.this.notify(true,"onPhyRead "+status);
            }
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            BTReceiver.this.notify(true,"onDescriptorRead "+status);
            }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            BTReceiver.this.notify(true,"onDescriptorWrite "+status);
            }
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            BTReceiver.this.notify(true,"onReadRemoteRssi "+status);
            }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            BTReceiver.this.notify(true,"onMtuChanged "+status);
            }
    };
    private UUID retryUUID(UUID uuid){
        return new UUID(uuid.getMostSignificantBits(),uuid.getLeastSignificantBits());
        }
    private void notifyUUID(UUID uuid){
        notify(true,"uuid="+uuid.toString()+"\n"+Long.toHexString(uuid.getMostSignificantBits())+" "+Long.toHexString(uuid.getLeastSignificantBits()));
        }
    private boolean procReceived(byte dd[]) {
        if (dd==null || dd.length!=20)
            return false;
        for(int i=0;i<10;i++)
            buffer[i]= (short)( dd[2*i] & 0x0FF | (dd[2*i+1]<<8) & 0x0FF00);
        //notify("Принято: "+buffer[0]+":"+buffer[1]);
        switch (buffer[0]){
case SENSOR_ANS_CHARGE_LEVEL:
            notify(false,"заряд батареи: "+buffer[1]+" %");
            return false;
case SENSOR_ANS_STOP:
            if (buffer[1]==0){
                 notify(false,"отмена измерения");
                 return false;
                 }
            if (buffer[1]!=data.length){
                 notify(false,"несовпадение размера блока");
                 return false;
                 }
            file.setData(data);
            data = null;
            //------------ Убрать синхронизацию к GUI ---------------------
            //activity.runOnUiThread(new Runnable() {
            //            @Override
            //            public void run() {
            back.onReceive(BTReceiver.this,file);
            back.onStateText(BTReceiver.this,"");
            //            }
            //        });
            return false;
case SENSOR_ANS_ERROR:
            notify(false,"ошибка исполнения команды: "+errorCodes[buffer[1]]);
            back.onState(BTReceiver.this,BTViewFace.BT_LightRed);
            return false;
case SENSOR_ANS_DATA:
            for(int i=buffer[1],j=2; j<10; i++,j++)
                data[i]=buffer[j];
            back.onStateText(this,""+buffer[1]*100/data.length+" %");
            return true;
            }
    return false;
    }
//------------------------------------------- Пользовательская часть -------------------------------
    public void blueToothOff(){
        if (BLEisOn){
            btClose();
            }
        back.onState(BTReceiver.this,BTViewFace.BT_Gray);
        }
    public void blueToothOn(BluetoothDevice device0){
        device = device0;
        sensorName = device.getName();
        sensorMAC = device.getAddress();
        notify(true,": выбран: "+device.getName()+" "+device.getAddress());
        gatt = device.connectGatt(activity, false, gattBack,TRANSPORT_LE);
        gatt.connect();
        back.onState(BTReceiver.this,BTViewFace.BT_Yellow);
        BLEisOn = true;
        }
    public boolean isBlueToothOn(){
        return BLEisOn;
    }
    
}
