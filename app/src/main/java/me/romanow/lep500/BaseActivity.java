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

import romanow.snn_simulator.fft.FFT;
import romanow.snn_simulator.fft.FFTAudioTextFile;
import romanow.snn_simulator.fft.FFTParams;
import romanow.snn_simulator.layer.LayerStatistic;

public abstract class BaseActivity extends AppCompatActivity {
    private LineGraphView multiGraph=null;
    public final static int greatTextSize=20;             // Крупный шрифт
    public final static int KF100=FFT.sizeHZ/100;
    private final int paintColors[]={0x0000FF00,0x000000FF,0x0000FFFF,0x00FF00FF};
    private int colorNum=0;
    public abstract void addToLog(String ss, int textSize);
    public abstract void addToLogHide(String ss);
    public abstract void popupAndLog(String ss);
    public abstract void showStatisticFull(LayerStatistic inputStat);
    public void addToLog(String ss){
        addToLog(ss,0);
        }
    public final String androidFileDirectory(){
        return getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        }
    //--------------------------------------------------------------------------
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
    public LinearLayout createMultiGraph(int resId){
        colorNum=0;
        LinearLayout lrr=(LinearLayout)getLayoutInflater().inflate(resId, null);
        LinearLayout panel = (LinearLayout)lrr.findViewById(R.id.viewPanel);
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
    public synchronized void addGraphView(LayerStatistic inputStat){
        paintOne(multiGraph,inputStat.getMids(),paintColors[colorNum],0,0,true);
        colorNum++;
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
                FFTWindowReduce(false).p_Cohleogram(false).p_GPU(false).compressMode(false).
                winMode(set.winFun);
        FFT fft = new FFT();
        fft.setFFTParams(params);
        fft.calcFFTParams();
        double freqStep = fft.getStepHZLinear()/KF100;
        addToLogHide("Отсчетов "+xx.getFrameLength());
        addToLogHide("Кадр: "+set.p_BlockSize*FFT.Size0);
        addToLogHide("Перекрытие: "+set.p_OverProc);
        addToLogHide("Дискретность: "+String.format("%5.4f",freqStep)+" гц");
        //inputStat.reset();
        //fft.fftDirect(xx,back);
        fft.fftDirect(xx,new FFTAdapter(this,title));
    }

}
