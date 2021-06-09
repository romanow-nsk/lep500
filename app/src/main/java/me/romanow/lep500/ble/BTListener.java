package me.romanow.lep500.ble;

import me.romanow.lep500.LEP500File;

public interface BTListener {
    public void notify(BTReceiver sensor, boolean fullInfo, String ss);
    public void onReceive(BTReceiver sensor, LEP500File file);
    public void onState(BTReceiver sensor, int state);
    public void onStateText(BTReceiver sensor, String text);
    public void onPopup(BTReceiver sensor, String text);
}
