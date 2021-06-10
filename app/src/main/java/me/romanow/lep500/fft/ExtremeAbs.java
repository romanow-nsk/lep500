package me.romanow.lep500.fft;

public class ExtremeAbs extends ExtremeFacade {
    @Override
    public String getTitle() {
        return "Амплитуда";
        }
    @Override
    public double getValue() {
        return extreme.value;
        }
}
