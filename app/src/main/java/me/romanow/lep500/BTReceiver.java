package me.romanow.lep500;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;

public class BTReceiver extends Thread{
    private final byte UUID_COMMON[]={0x00,0x00,(byte)0xfe,0x40,(byte)0xcc,0x7a,0x48,0x2a,
            (byte)0x98,0x4a,0x7f,0x2e,(byte)0xd5,(byte)0xb3,(byte)0xe5,(byte)0x8f};
    private final byte UUID_WRITE[]={0x00,0x00,(byte)0xfe,0x41,(byte)0x8e,0x22,0x45,0x41,
            (byte)0x9d,0x4c,0x21,(byte)0xed,(byte)0xae,(byte)0x82,(byte)0xed,0x19};
    private final byte UUID_READ[]={0x00,0x00,(byte)0xfe,0x42,(byte)0x8e,0x22,0x45,0x41,
            (byte)0x9d,0x4c,0x21,(byte)0xed,(byte)0xae,(byte)0x82,(byte)0xed,0x19};
    private final int SENSOR_CMD_START=1;           // Команда - начать измерения
    private final int SENSOR_CMD_STOP=2;            // Команда - прервать измерения
    private final int SENSOR_CMD_CHARGE_LEVEL=6;    // Команда - получить заряд батереи
    private final int SENSOR_CMD_OFF=8;             // Команда - отключить сенсор
    private final int SENSOR_ANS_DATA=3;            // Ответ - блок данных, параметр = смещение
    private final int SENSOR_ANS_STOP=4;            // Ответ - завершение измерения, параметр 0 - прервано, иначе- кол-во байтов
    private final int SENSOR_ANS_CHARGE_LEVEL=6;    // Ответ - параметр = заряд батереи
    private final int SENSOR_ANS_ERROR=5;           // Ответ - параметр = код ошибки
    private final int WAIT_FOR_READ=10;             // Цикл засыпания при отсутствии данных
    //==============================================================================================
    private LEP500File file;
    private BTListener back;
    private InputStream is;
    private OutputStream os;
    private BluetoothDevice device;
    private BluetoothSocket rdSocket;
    private BluetoothSocket wrSocket;
    private Activity activity;
    private short buffer[] = new short[10];
    private short data[]=null;
    private boolean shutdown=false;
    private BluetoothGatt gatt;
    public synchronized boolean isWorking() {
        return data != null;
        }
    public void btClose(){
        try {
            os.close();
            is.close();rdSocket.close();
            wrSocket.close();
            }catch (Exception ee){}
        }
    public BTReceiver(Activity activity0, LEP500File file0, BluetoothDevice device0, BTListener back0){
        back = back0;
        file = file0;
        device = device0;
        activity = activity0;
        shutdown=false;
        notify(UUID.nameUUIDFromBytes(UUID_COMMON).toString());
        notify(UUID.nameUUIDFromBytes(UUID_READ).toString());
        notify(UUID.nameUUIDFromBytes(UUID_WRITE).toString());
        gatt = device.connectGatt(activity, false, gattBack);
        gatt.connect();
        gatt.discoverServices();
        }
    public void sendCommand(int cmd, int param)throws Exception{
        os.write(cmd);
        os.write(cmd>>8);
        os.write(param);
        os.write(param>>8);
        os.flush();
        }
    public synchronized void startMeasure(int duration) throws Exception {
        if (isWorking()){
            notify("Измерение уже выполняется");
            return;
            }
        duration *=100;
        duration -= duration%8;             // Кратность 8;
        data = new short[duration];
        sendCommand(SENSOR_CMD_START,duration);
        }
    public void stopMeasure() throws Exception {
        if (!isWorking()){
            notify("Измерение не выполняется");
            return;
            }
        sendCommand(SENSOR_CMD_STOP,0);
        }
    public synchronized void shutdown(){
        shutdown = true;
        interrupt();        // Нужно ли????
        }
    public int readByte() throws Exception{
        while(is.available()==0){
            Thread.sleep(WAIT_FOR_READ);
            if (shutdown)
                throw new IOException("Закрытие соединенеия");
            }
        int xx = is.read();
        return xx;
        }
    public short readShort() throws Exception{
        int xx = readByte() & 0x0FF;
        xx |= (readByte() & 0x0FF) << 8;
        return (short)xx;
        }
    public void readBlock() throws Exception{
        for(int i=0; i<10;i++)
            buffer[i] = readShort();
        }
    private void notify(final String mes){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                back.notify(mes);
                    }
            });
        }
    private String errorCodes[]={"","Недопустимый интервал измерения","Отмена при отсутствии измерений","Неизвестная команда"};
    BluetoothGattCallback gattBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    BTReceiver.this.notify("Устройство включилось");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    BTReceiver.this.notify("Устройство отключилось");
                    break;
                default:
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            for(BluetoothGattService service : services){
                BTReceiver.this.notify(service.getUuid().toString());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics())
                    gatt.readCharacteristic(characteristic);
                }
            }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic  characteristic, int status) {
            byte bb[] = characteristic.getValue();
            BTReceiver.this.notify("Принято: "+bb.length+" "+characteristic.getStringValue(0));
            }
    };
    @Override
    public void run() {
        try {
            gatt.discoverServices();
            BluetoothGattService service = gatt.getService(UUID.nameUUIDFromBytes(UUID_COMMON));
            BluetoothGattCharacteristic wrCharacteristic = service.getCharacteristic(UUID.nameUUIDFromBytes(UUID_WRITE));
            List<BluetoothGattService> services = gatt.getServices();
            Thread.sleep(10000);
            rdSocket = device.createRfcommSocketToServiceRecord(UUID.nameUUIDFromBytes(UUID_WRITE));
            wrSocket = device.createRfcommSocketToServiceRecord(UUID.nameUUIDFromBytes(UUID_READ));
            rdSocket.connect();
            wrSocket.connect();
            is = rdSocket.getInputStream();
            os = wrSocket.getOutputStream();
            notify("Подключение выполнено");
            // TODO - протокол приема
            sendCommand(0,1);       // Пока для зажигания диода
            while (!shutdown){
                readBlock();
                switch (buffer[0]){
case SENSOR_ANS_CHARGE_LEVEL:
                    notify("Заряд батареи: "+buffer[1]+" %");
                    break;
case SENSOR_ANS_STOP:
                    if (buffer[1]==0){
                        notify("Отмена измерения");
                        return;
                        }
                    if (buffer[1]!=data.length*2){
                        notify("Несовпадение размера блока");
                        return;
                        }
                    final short ss[] = data;
                    data = null;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            back.onReceive(ss);
                        }
                    });
                    break;
case SENSOR_ANS_ERROR:
                   notify("Ошибка исполнения команды: "+errorCodes[buffer[1]]);
                   break;
case SENSOR_ANS_DATA:
                    for(int i=buffer[1]/2,j=2; j<10; i++,j++)
                        data[i]=buffer[j];
                    break;
                    }
                }
            } catch (final Exception e) {
                notify("Ошибка: "+e.toString());
                }
    }
}
