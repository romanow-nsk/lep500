package me.romanow.lep500.fft;

public class ExtremeDiff extends ExtremeFacade {
    @Override
    public String getTitle() {
        return "Пик по спаду";
        }
    @Override
    public double getValue() {
        return extreme.diff;
        }
}
