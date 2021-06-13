package me.romanow.lep500;

import me.romanow.lep500.fft.FFT;
import me.romanow.lep500.fft.FFTCallBack;
import me.romanow.lep500.fft.FFTStatistic;

import static me.romanow.lep500.MainActivity.createFatalMessage;

public class FFTAdapter implements FFTCallBack {
    private FFTStatistic inputStat;
    private BaseActivity main;
    public FFTAdapter(BaseActivity main0, String title){
        inputStat = new FFTStatistic(title);
        main = main0;
        }
    @Override
    public void onStart(double msOnStep) {}
    @Override
    public void onFinish() {
        if (inputStat.getCount()==0){
            main.popupAndLog("Настройки: короткий период измерений/много блоков");
            return;
            }
        inputStat.smooth(AppData.ctx().set().kSmooth);
        main.defferedAdd(inputStat);
        }
    @Override
    public boolean onStep(int nBlock, int calcMS, double totalMS, FFT fft) {
        long tt = System.currentTimeMillis();
        double lineSpectrum[] = fft.getSpectrum();
        boolean xx;
        try {
            inputStat.addStatistic(lineSpectrum);
        } catch (Exception ex) {
            main.addToLog(createFatalMessage(ex,10));
            return false;
        }
        return true;
    }
    @Override
    public void onError(Exception ee) {
        main.addToLog(createFatalMessage(ee,10));
    }
    @Override
    public void onMessage(String mes) {
        main.addToLogHide(mes);
        }

}
