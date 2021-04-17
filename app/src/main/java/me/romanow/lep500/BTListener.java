package me.romanow.lep500;

public interface BTListener {
    public void notify(String ss);
    public void onReceive(LEP500File file);
}
