package me.romanow.lep500.ble;

import java.util.ArrayList;

import me.romanow.lep500.ble.BTReceiver;

public interface SensorGroupListener {
    public void onSensor(ArrayList<BTReceiver> receiver);
}
