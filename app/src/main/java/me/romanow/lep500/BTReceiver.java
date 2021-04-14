package me.romanow.lep500;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public abstract class BTReceiver extends Thread{
    protected LEP500File file;
    protected ValueListener back;
    private InputStream is;
    private OutputStream os;
    protected BluetoothDevice device;
    protected BluetoothSocket socket;
    private Activity activity;
    protected UUID uuid;
    public abstract BluetoothSocket getSocket() throws Exception;
    public abstract void btClose();
    public BTReceiver(UUID uuid0, Activity activity0, LEP500File file0, BluetoothDevice device0, ValueListener back0){
        uuid = uuid0;
        back = back0;
        file = file0;
        device = device0;
        activity = activity0;
        start();
        }
    public int readInt() throws Exception{
        int vv=0;
        for(int i=0;i<4;i++){
            while(is.available()==0){
                Thread.sleep(1000);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        back.onValue(true,"Ожидание");
                    }
                });
                }
            int xx = is.read();
            vv |= xx<<(i*8);
            }
        return vv;
        }
    @Override
    public void run() {
        try {
            socket = getSocket();
            is = socket.getInputStream();
            os = socket.getOutputStream();
            // TODO - протокол приема
            int sz = readInt();
            file.createData(sz);
            float data[] = file.getData();
            for(int i=0; i<sz;i++)
                data[i] = Float.intBitsToFloat(readInt());
            is.close();
            os.close();
            socket.close();
            btClose();
            } catch (final Exception e) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        back.onValue(false,"Ошибка соединения: "+e.toString());
                    }
                });
                }
    }
}
