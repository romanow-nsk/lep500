package me.romanow.lep500.fft;

public class Extreme {
    public final double value;
    public final double diff;
    public final double trend;
    public final int idx;
    public final int decSize;
    public Extreme(double value, int idx,double diff,double trend,int decSize) {
        this.value = value;
        this.diff = diff;
        this.idx = idx;
        this.trend = trend;
        this.decSize = decSize;
    }
}
