package me.romanow.lep500;

import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import me.romanow.lep500.fft.FFT;
import me.romanow.lep500.fft.FFTAudioTextFile;
import me.romanow.lep500.fft.FFTParams;
import me.romanow.lep500.fft.FFTStatistic;

public abstract class BaseActivity extends AppCompatActivity {
    private LineGraphView multiGraph=null;
    public final static int greatTextSize=20;             // Крупный шрифт
    public final static int KF100=FFT.sizeHZ/100;
    private final static int paintColors[]={0x0000A000,0x000000FF,0x0000A0A0,0x00C000C0};
    protected double freqStep=0;
    public abstract void clearLog();
    public abstract void addToLog(String ss, int textSize);
    public abstract void addToLogHide(String ss);
    public abstract void popupAndLog(String ss);
    public abstract void showStatisticFull(FFTStatistic inputStat, int idx);
    public void addToLog(String ss){
        addToLog(ss,0);
        }
    public final String androidFileDirectory(){
        return getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        }
    //--------------------------------------------------------------------------
    public int getPaintColor(int idx){
        if (idx < paintColors.length)
            return paintColors[idx];
        idx -= paintColors.length;
        int color = 0x00A0A0A0;
        while(idx--!=0 && color!=0)
            color-=0x00202020;
        return color;
        }
    public void paintOne(float data[], int color){
        paintOne(multiGraph,data,color,0,0,false);
        }
    public void paintOne(LineGraphView graphView, float data[], int color, int noFirst, int noLast, boolean freqMode){
        GraphView.GraphViewData zz[] = new GraphView.GraphViewData[data.length-noFirst-noLast];
        for(int j=noFirst;j<data.length-noLast;j++){                    // Подпись значений факторов j-ой ячейки
            double freq = freqMode ? (j*50./data.length) : (j/100.);
            zz[j-noFirst] = new GraphView.GraphViewData(freq,data[j]);
            }
        GraphViewSeries series = new GraphViewSeries(zz);
        series.getStyle().color = color | 0xFF000000;
        graphView.addSeries(series);
        }
    public LinearLayout createMultiGraph(int resId,double procHigh){
        LinearLayout lrr=(LinearLayout)getLayoutInflater().inflate(resId, null);
        LinearLayout panel = (LinearLayout)lrr.findViewById(R.id.viewPanel);
        if (procHigh!=0){
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)panel.getLayoutParams();
            params.height = (int)(getResources().getDisplayMetrics().widthPixels*procHigh);
            panel.setLayoutParams(params);
            }
        multiGraph = new LineGraphView(this,"");
        multiGraph.setScalable(true);
        multiGraph.setScrollable(true);
        multiGraph.getGraphViewStyle().setTextSize(15);
        panel.addView(multiGraph);
        return lrr;
        }
    public LineGraphView getMultiGraph() {
        return multiGraph;
        }
    //--------- Отложенная отрисовка с нормализацией --------------------------------------------------
    private ArrayList<FFTStatistic> deffered = new ArrayList<>();
    public void defferedStart(){
        deffered.clear();
        }
    public void defferedFinish(){
        normalize();
        for(int i=0;i<deffered.size();i++){
            showStatisticFull(deffered.get(i),i);
            paintOne(multiGraph,deffered.get(i).getNormalized(),getPaintColor(i),0,0,true);
            }
        }
    public void defferedAdd(FFTStatistic inputStat){
        deffered.add(inputStat);
        }
    private void normalize(){
        double max=0;
        for(FFTStatistic statistic : deffered) {
            float max2 = statistic.normalizeStart();
            if (max2 > max)
                max = max2;
            }
        for(FFTStatistic statistic : deffered) {
            statistic.normalizelFinish((float) max);
            }
        }
    //-----------------------------------------------------------------------------------------------------
    public synchronized void addGraphView(FFTStatistic inputStat, int idx){
        paintOne(multiGraph,inputStat.getMids(),getPaintColor(idx),0,0,true);
        }
    public void procArchive(FileDescription fd){
        String fname = fd.originalFileName;
        try {
            FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+fname);
            addToLog(fd.toString(),greatTextSize);
            processInputStream(fis,fd.toString());
           } catch (Throwable e) {
            addToLog("Файл не открыт: "+fname+"\n"+createFatalMessage(e,10));
            }
        }
    public static String createFatalMessage(Throwable ee, int stackSize) {
        String ss = ee.toString() + "\n";
        StackTraceElement dd[] = ee.getStackTrace();
        for (int i = 0; i < dd.length && i < stackSize; i++) {
            ss += dd[i].getClassName() + "." + dd[i].getMethodName() + ":" + dd[i].getLineNumber() + "\n";
            }
        String out = "Программная ошибка:\n" + ss;
        return out;
        }
    public void processInputStream(InputStream is, String title) throws Throwable{
        LEP500Settings set = AppData.ctx().set();
        FFTAudioTextFile xx = new FFTAudioTextFile();
        xx.setnPoints(set.nTrendPoints);
        xx.readData(new BufferedReader(new InputStreamReader(is, "Windows-1251")));
        xx.removeTrend(set.nTrendPoints);
        long lnt = xx.getFrameLength();
        //for(p_BlockSize=1;p_BlockSize*FFT.Size0<=lnt;p_BlockSize*=2);
        //if (p_BlockSize!=1) p_BlockSize/=2;
        FFTParams params = new FFTParams().W(set.p_BlockSize* FFT.Size0).procOver(set.p_OverProc).
                compressMode(false).winMode(set.winFun);
        FFT fft = new FFT();
        fft.setFFTParams(params);
        fft.calcFFTParams();
        freqStep = fft.getStepHZLinear()/KF100;
        addToLogHide("Отсчетов "+xx.getFrameLength());
        addToLogHide("Кадр: "+set.p_BlockSize*FFT.Size0);
        addToLogHide("Перекрытие: "+set.p_OverProc);
        addToLogHide("Дискретность: "+String.format("%5.4f",freqStep)+" гц");
        fft.fftDirect(xx,new FFTAdapter(this,title));
    }

}
