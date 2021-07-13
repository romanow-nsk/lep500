package me.romanow.lep500.fft;

import java.io.IOException;

import me.romanow.lep500.fft.FFTAudioSource;

public class FFTHarmonic implements FFTAudioSource {
    long size;              // Кол-во отсчетов
    long cnum=0;            // текущий номер отсчета
    int ngarm=1;
    double hzBase=0;
    int trendPeriod=0;
    double period[];
    double ampl[];
    public FFTHarmonic(double hzBase0, double hz[], double ampl0[], int sec, double hzTrend){
        hzBase = hzBase0;
        period = new double[hz.length];
        ampl = ampl0;
        for(int i=0;i<period.length;i++)
            period[i] = hzBase/hz[i];
        trendPeriod = (int)(hzBase/hzTrend);
        size = (int)(hzBase * sec);
        }
    @Override
    public String testSource(int sizeHZ) {
        return null;
    }
    @Override
    public long getFrameLength() {
        return size;
    }
    @Override
    public int read(double[] buf, int offset, int lnt) throws IOException {
        cnum = offset;
        int l = lnt;                // в отсчетах
        int trend=0;
        while(l--!=0){
            float sum=0;
            for(int j=0;j<period.length;j++)
                sum += ampl[j]*Math.sin(2*Math.PI * cnum /(period[j]));
            sum += trend/trendPeriod;
            buf[offset++] = sum;
            cnum++;
            trend++;
            if (trend > trendPeriod)
                trend=0;
            }
        return lnt;
    }
    @Override
    public void close() throws IOException {}
    public double getSampleRate() {
        return hzBase;
    }
}
