package me.romanow.lep500.fft;

public abstract class ExtremeFacade {
    Extreme extreme;
    public abstract String getTitle();
    public abstract double getValue();
    public void serExtreme(Extreme extreme){
        this.extreme = extreme;
        }
    public Extreme extreme(){ return extreme; }
}
