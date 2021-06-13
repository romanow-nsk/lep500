package me.romanow.lep500.fft;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import me.romanow.lep500.Utils;

public class FFT {
    public final static int  Size0= 1024;           // 1024 базовая степень FFT
    private double stepHZLinear;                     // Шаг лин.спектра
    private double totalMS=0;                        // Текущий момент времени
    private double stepMS;                           // Сдвиг окна (мс)
    private boolean preloadMode=false;              // Режим предварительной загрузки
    private int nblock=0;
    private FFTParams pars = new FFTParams();       //
    private double wave[]=null;                      // Текущая волна
    private FFTArray spectrum=null;                 // Текущий спектр
    private double fullWave[]=null;                  // preload волна
    private FFTAudioSource audioInputStream=null;
    private Complex[] complexSpectrum=null;
    private FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
    //---------------------------------------------------------------------------------------------
    public final static int WinModeRectangle=0;     // Прямоугольное окно
    public final static int WinModeTriangle=1;      // Треугольное окно
    public final static int WinModeSine=2;          // Синусное окно
    public final static int WinModeWelch=3;         // Параболическое окно
    public final static ArrayList<String> winFuncList=new ArrayList<>();
        {
        winFuncList.add("Прямоугольник");
        winFuncList.add("Треугольник");
        winFuncList.add("Синус");
        winFuncList.add("Парабола");
        }
    //-------------------------------------------------------------------------------------------
    public double[] getSpectrum() {
        return spectrum.getOriginal(); }
    //------------------------------ Функция окна -----------------------------
    private double winFun(double val, int idx){
        double sz = pars.W();
        switch (pars.winMode()){
            case WinModeRectangle: return val;
            case WinModeTriangle: return val*(1-Math.abs((idx-sz/2)/(sz/2)));
            case WinModeWelch:    double dd= Math.abs((idx-sz/2)/(sz/2));
                return val*(1-dd*dd);
            case WinModeSine:     return val*Math.sin(Math.PI*idx/sz);
            }
        return val;
        }
    //--------------------------------------------------------------------------
    private TimeCounter tc =
            new TimeCounter(new String[]{"Чтение","БПФ","Компрессия","Общее"});
    public TimeCounter getTimeCounter() {
        return tc; }
    public void addCount(int idx){
        tc.addCount(idx);
        }
    public void setFFTParams(FFTParams pars){
        this.pars = pars;
        calcFFTParams();
        }
    public void calcFFTParams(){
        stepHZLinear = pars.freqHZ()/pars.W();
        stepMS = 10*pars.W()*(100-pars.procOver())/pars.freqHZ();
        totalMS=0;
        Utils.calcExp();
        }
    public double getStepHZLinear() {
        return stepHZLinear;
        }
    public int getAudioLength(FFTAudioSource audioInputStream, FFTCallBack back){
        if (audioInputStream==null){
            back.onError(new Exception("Не выбран источник аудио"));
            return -1;
        }
        this.audioInputStream = audioInputStream;
        back.onMessage("Длина "+audioInputStream.getFrameLength()+
                " дискретность "+stepHZLinear+" гц");
        int size = (int)audioInputStream.getFrameLength();
        return size;
        }
    public  boolean preloadWave(FFTAudioSource audioInputStream, FFTCallBack back){
        calcFFTParams();
        int size = getAudioLength(audioInputStream, back);
        if (size == -1)
            return false;
        fullWave = new double[size];
        try {
            audioInputStream.read(fullWave, 0, size);
           } catch(IOException ee){
                back.onError(ee);
                return false;
            }
        return true;
    }
    // kOver - коэфф. перекрытия скользящего буфера при последующем чтении
    public void fftDirect(FFTAudioSource audioInputStream, FFTCallBack back){
        if (!preloadWave(audioInputStream,back))
            return;
        preloadMode=false;
        tc.clear();
        back.onStart(stepMS);
        wave = new double[pars.W()];
        spectrum = new FFTArray(wave.length/2);
        nblock=0;
        int nQuant = pars.W()*(100-pars.procOver())/100;
        int sz = fullWave.length/nQuant;
        try {
            int read=0;
            int lnt=0;
            tc.clear();
            for(int i=0, base=0; i<sz; i++, base+=nQuant){
                spectrum = new FFTArray(wave.length/2);
                tc.fixTime();
                for(int j=0;j<pars.W();j++){
                    wave[j] = (float)winFun(base+j<fullWave.length ? fullWave[base+j] : 0,j);
                    }
                tc.addCount(0);
                fftDirectStandart(); // Переменный (фикс.) размер окна
                tc.addCount(1);
                spectrum.compress(pars.compressMode(), pars.compressGrade(),pars.kAmpl());
                tc.addCount(2);
                //spectrum.nextStep();
                //GTSpectrum.nextStep();
                boolean bb = back.onStep(nblock, tc.getTotal(), totalMS, this);
                nblock++;
                if (!bb)
                    break;
                tc.addCountTotal(3);
                totalMS += stepMS;
            }
            back.onMessage("Блоков "+nblock);
            back.onMessage(tc.toString());
            close(back);
        } catch (Exception e) {
            back.onError(e);
            close(back);
            }
        }
    /*
    public void fftDirectStandartIterative(){
        fftDirectStandart();
        float tmp[] = wave;                 // Укорочение интервалов для высоких частот
        int pow = 1;                        // ПО ОКТАВАМ
        int oct=2;                          // Частота, с которой копируется спектр
        int base=0;
        int nextBase = toneIndexes[pars.subToneCount()*12*oct];
        float radice = (float)(1 / Math.sqrt(tmp.length));
        while(tmp.length>=2018){            //
            Complex xx[] = fft.transform(convert(tmp), TransformType.FORWARD);
            if (pow==1)
                complexSpectrum = xx;
            //float radice = (float)(1 / Math.sqrt(tmp.length));
            for(int i = base; i < tmp.length/2 + 1 && i<nextBase; i++){
                for(int j=0;j<pow;j++)
                    spectrum.set(i*pow+j, (float)(xx[i].abs()*radice));
                }
            tmp = reduceTo(tmp);
            pow*=2;
            base = nextBase;
            nextBase*=2;
        }
    }
    */
    public void fftDirectStandart(){
        long timeStart = new Date().getTime();
        complexSpectrum = fft.transform(Utils.convert(wave),TransformType.FORWARD);
        float radice = (float)(1 / Math.sqrt(wave.length));
        for(int i = 0; i < wave.length/2 + 1; i++){
            spectrum.set(i, (float)complexSpectrum[i].abs()*radice);
            }
        }
    public void close(FFTCallBack back){
        try {
            if (audioInputStream!=null)
                audioInputStream.close();
            audioInputStream = null;
            } catch (IOException ex) {}
                if (back!=null)
                    back.onFinish();
                }
    public void close(){
        close(null);
        }
}
