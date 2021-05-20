package me.romanow.lep500;

import romanow.snn_simulator.fft.FFT;
import romanow.snn_simulator.fft.FFTCallBack;
import romanow.snn_simulator.layer.LayerStatistic;

import static me.romanow.lep500.MainActivity.createFatalMessage;

public class FFTAdapter implements FFTCallBack {
    private LayerStatistic inputStat;
    private GraphBaseActivity main;
    public FFTAdapter(GraphBaseActivity main0,String title){
        inputStat = new LayerStatistic(title);
        main = main0;
        }
    @Override
    public void onStart(float msOnStep) {}
    @Override
    public void onFinish() {
        if (inputStat.getCount()==0){
            main.popupAndLog("Настройки: короткий период измерений/много блоков");
            return;
            }
        inputStat.smooth(AppData.ctx().set().kSmooth);
        main.showStatisticFull(inputStat);
        main.addGraphView(inputStat);
        main.addToLog("");
        }
    @Override
    public boolean onStep(int nBlock, int calcMS, float totalMS, FFT fft) {
        long tt = System.currentTimeMillis();
        float lineSpectrum[] = fft.getSpectrum();
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
