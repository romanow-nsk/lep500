package me.romanow.lep500;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import romanow.snn_simulator.fft.FFT;
import romanow.snn_simulator.fft.FFTAudioTextFile;
import romanow.snn_simulator.fft.FFTCallBack;
import romanow.snn_simulator.fft.FFTParams;
import romanow.snn_simulator.layer.Extreme;
import romanow.snn_simulator.layer.LayerStatistic;

public class MainActivity extends AppCompatActivity {
    private FFT fft = new FFT();
    private LayerStatistic inputStat = new LayerStatistic("Входные данные");
    private final int  p_BlockSize=8;
    private final int  p_OverProc=50;
    private final boolean  p_LogFreq=false;
    private final boolean  p_Compress=false;
    private final int  compressLevel=0;
    private final int  p_SubToneCount=1;
    private int     kSmooth=50;             // Циклов сглаживания
    private int nFirstMax=10;               // Количество максимумов в статистике (вывод)
    private int noFirstPoints=30;           // Отрезать точек справа и слева
    private int noLastPoints=3000;
    private float kMultiple=3.0f;
    private float kAmpl=1;
    private int nTrendPoints=100;             // Точек при сглаживании тренда =0 - отключено
    private final int KF100=FFT.sizeHZ/100;
    private final int MiddleMode=0x01;
    private final int DispMode=0x02;
    private final int MiddleColor = 0x0000FF00;
    private final int DispColor = 0x000000FF;
    private final int GraphBackColor = 0x00A0C0C0;
    //----------------------------------------------------------------------------
    private LinearLayout log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            log = (LinearLayout) findViewById(R.id.log);
            } catch (Exception ee){ addToLog(createFatalMessage(ee,10));}
    }

    private void addToLog(String ss){
        TextView txt = new TextView(this);
        txt.setText(ss);
        log.addView(txt);
        }


    private final int CHOOSE_RESULT=100;
    private void preloadFromText(){
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("text/plain");
        intent = Intent.createChooser(chooseFile, "Выбрать txt");
        startActivityForResult(intent, CHOOSE_RESULT);
        }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        String path     = "";
        if(requestCode == CHOOSE_RESULT) {
            try {
                Uri uri = data.getData();
                FFTAudioTextFile xx = new FFTAudioTextFile();
                xx.setnPoints(nTrendPoints);
                xx.readData(new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), "Windows-1251")));
                fft.setFFTParams(new FFTParams(p_BlockSize*FFT.Size0,p_OverProc, p_LogFreq,p_SubToneCount, false, false,false,0,kMultiple));
                String ss = uri.getLastPathSegment();
                int idx= ss.lastIndexOf("/");
                if (idx!=-1) ss = ss.substring(idx+1);
                addToLog("Файл: "+ss);
                addToLog("Отсчетов "+xx.getFrameLength());
                fft.setLogFreqMode(p_LogFreq);
                fft.setCompressMode(p_Compress);
                fft.setCompressGrade(compressLevel);
                fft.setKAmpl(kAmpl);
                inputStat.reset();
                fft.fftDirect(xx,back);
                } catch (Exception ee){
                    addToLog(createFatalMessage(ee,10));
                    }
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

    private void showExtrems(boolean mode){
        int sz = inputStat.getMids().length;
        addToLog(String.format("Диапазон экстремумов: %6.4f-%6.4f",100./sz*noFirstPoints,50./sz*(sz-noLastPoints)));
        ArrayList<Extreme> list = inputStat.createExtrems(mode,noFirstPoints,noLastPoints);
        if (list.size()==0){
            addToLog("Экстремумов не найдено");
            return;
            }
        int count = nFirstMax < list.size() ? nFirstMax : list.size();
        Extreme extreme = list.get(0);
        double val0 = mode ? extreme.value : extreme.diff;
        addToLog(mode ? "По амплитуде" : "По спаду");
        addToLog(String.format("Макс=%6.4f f=%6.4f гц",extreme.value,extreme.freq/KF100));
        double sum=0;
        for(int i=1; i<count;i++){
            extreme = list.get(i);
            double proc = (mode ? extreme.value : extreme.diff)*100/val0;
            sum+=proc;
            addToLog(String.format("Макс=%6.4f f=%6.4f гц %d%% к первому",extreme.value,extreme.freq/KF100,(int)proc));
            }
        addToLog(String.format("Средний - %d%% к первому",(int)(sum/(count-1))));
        }

    private void showStatistic(){
        String out = "Отсчетов:"+inputStat.getCount()+"\n";
        double mid =inputStat.getMid();
        out+=String.format("Среднее:%6.4f\n",mid);
        out+=String.format("Приведенное станд.откл:%6.4f\n",inputStat.getDisp()/mid);
        out+=String.format("Приведенная неравн.по T:%6.4f\n",inputStat.getDiffT()/mid);
        out+=String.format("Приведенная неравн.по F:%6.4f\n",inputStat.getDiffF()/mid);
        addToLog(out);
        showExtrems(true);
        showExtrems(false);
        }
    //--------------------------------------------------------------------------
    private FFTCallBack back = new FFTCallBack(){
        @Override
        public void onStart(float msOnStep) {
            }
        @Override
        public void onFinish() {
            inputStat.smooth(kSmooth);
            showStatistic();
            addGraphView(inputStat,MiddleMode);
            addToLog("-------------------------");
            }
        @Override
        public boolean onStep(int nBlock, int calcMS, float totalMS, FFT fft) {
                long tt = System.currentTimeMillis();
                float lineSpectrum[] = fft.getSpectrum();
                boolean xx;
                try {
                    inputStat.addStatistic(lineSpectrum);
                    } catch (Exception ex) {
                        addToLog(createFatalMessage(ex,10));
                        return false;
                        }
                return true;
            }
        @Override
        public void onError(Exception ee) {
            addToLog(createFatalMessage(ee,10));
            }
        @Override
        public void onMessage(String mes) {
            addToLog(mes);
            }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_loadTxt) {
            preloadFromText();
            return true;
            }
        if (id == R.id.action_settings) {
            return true;
            }
        if (id == R.id.action_clear) {
            log.removeAllViews();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //--------------------------------------------------------------------------------------------------------
    public void popupToast(int viewId, String ss) {
        Toast toast3 = Toast.makeText(getApplicationContext(), ss, Toast.LENGTH_LONG);
        LinearLayout toastContainer = (LinearLayout) toast3.getView();
        ImageView catImageView = new ImageView(getApplicationContext());
        TextView txt = (TextView)toastContainer.getChildAt(0);
        txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        txt.setGravity(Gravity.CENTER);
        catImageView.setImageResource(viewId);
        toastContainer.addView(catImageView, 0);
        toastContainer.setOrientation(LinearLayout.HORIZONTAL);
        toastContainer.setGravity(Gravity.CENTER);
        toastContainer.setVerticalGravity(5);
        toast3.setGravity(Gravity.TOP, 0, 200);
        toast3.show();
        }
    public void popupInfo(String ss) {
        popupToast(R.drawable.info,ss);
        }
    //--------------------------------------------------------------------------
    public void paintOne(LineGraphView graphView,float data[], int color){
        GraphView.GraphViewData zz[] = new GraphView.GraphViewData[data.length-noFirstPoints-noLastPoints];
        for(int j=noFirstPoints;j<data.length-noLastPoints;j++){                    // Подпись значений факторов j-ой ячейки
            double freq = j*50./data.length;
            zz[j-noFirstPoints] = new GraphView.GraphViewData(freq,data[j]);
            }
        GraphViewSeries series = new GraphViewSeries(zz);
        series.getStyle().color = color | 0xFF000000;
        graphView.addSeries(series);
        }


    private void addGraphView(LayerStatistic stat, int mode){
        LinearLayout lrr=(LinearLayout)getLayoutInflater().inflate(R.layout.graphview, null);
        LinearLayout panel = (LinearLayout)lrr.findViewById(R.id.viewPanel);
        LineGraphView graphView = new LineGraphView(this,"");
        //String axis[]={"0","10","20","30","40","50"};
        //graphView.setHorizontalLabels(axis);
        graphView.setScalable(true);
        graphView.setScrollable(true);
        graphView.getGraphViewStyle().setTextSize(15);
        panel.addView(graphView);
        log.addView(lrr);
        if ((mode & MiddleMode)!=0)
            paintOne(graphView,inputStat.getMids(),MiddleColor);
        if ((mode & DispMode)!=0)
            paintOne(graphView,inputStat.getDisps(),DispColor);
        }
}
