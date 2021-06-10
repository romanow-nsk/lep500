package me.romanow.lep500.fft;

public class ExtremeTrend extends ExtremeFacade {
    @Override
    public String getTitle() {
        return "Пик по тренду";
        }
    @Override
    public double getValue() {
        return extreme.trend;
        }
}
