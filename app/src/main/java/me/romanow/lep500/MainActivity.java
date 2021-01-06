package me.romanow.lep500;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.geometry.euclidean.twod.Line;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
    private final int greatTextSize=20;     // Кпупный шрифт
    private int     kSmooth=50;             // Циклов сглаживания
    private int nFirstMax=10;               // Количество максимумов в статистике (вывод)
    private int noFirstPoints=30;           // Отрезать точек справа и слева
    private int noLastPoints=3000;
    private float kMultiple=3.0f;
    private float kAmpl=1f;
    private int nTrendPoints=100;             // Точек при сглаживании тренда =0 - отключено
    private final int KF100=FFT.sizeHZ/100;
    private final int MiddleMode=0x01;
    private final int DispMode=0x02;
    private final int MiddleColor = 0x0000FF00;
    private final int DispColor = 0x000000FF;
    private final int GraphBackColor = 0x00A0C0C0;
    private boolean fullInfo=false;
    private boolean hideFFTOutput=false;
    private DataDesription archive = new DataDesription();
    //--------------------------------------------------------------------------
    final static String archiveFile="LEP500Archive.json";
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
        addToLog(ss,0);
        }
    private void addToLog(String ss, int textSize){
        TextView txt = new TextView(this);
        txt.setText(ss);
        if (textSize!=0)
            txt.setTextSize(textSize);
        log.addView(txt);
        }
    private void addToLogButton(String ss){
        addToLogButton(ss,null,null);
        }
    private void addToLogButton(String ss, View.OnClickListener listener){
        addToLogButton(ss,listener,null);
        }
    private void addToLogButton(String ss, View.OnClickListener listener, View.OnLongClickListener listenerLong){
        LinearLayout button = (LinearLayout)getLayoutInflater().inflate(R.layout.button,null);
        Button bb = (Button)button.findViewById(R.id.button_press);
        bb.setText(ss);
        bb.setTextSize(greatTextSize);
        if (listener!=null)
            bb.setOnClickListener(listener);
        if (listenerLong!=null)
            bb.setOnLongClickListener(listenerLong);
        log.addView(button);
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

    public void processInputStream(InputStream is) throws Exception{
        FFTAudioTextFile xx = new FFTAudioTextFile();
        xx.setnPoints(nTrendPoints);
        xx.readData(new BufferedReader(new InputStreamReader(is, "Windows-1251")));
        xx.removeTrend(nTrendPoints);
        fft.setFFTParams(new FFTParams(p_BlockSize*FFT.Size0,p_OverProc, p_LogFreq,p_SubToneCount, false, false,false,0,kMultiple));
        if (!hideFFTOutput)
            addToLog("Отсчетов "+xx.getFrameLength());
        fft.setLogFreqMode(p_LogFreq);
        fft.setCompressMode(p_Compress);
        fft.setCompressGrade(compressLevel);
        fft.setKAmpl(kAmpl);
        inputStat.reset();
        fft.fftDirect(xx,back);
        }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        String path     = "";
        if(requestCode == CHOOSE_RESULT) {
            try {
                Uri uri = data.getData();
                String ss = uri.getLastPathSegment();
                int idx= ss.lastIndexOf("/");
                if (idx!=-1) ss = ss.substring(idx+1);
                FileDescription description = new FileDescription(ss);
                String out = description.parseFromName();
                if (out!=null){
                    addToLog("Имя файла: "+out);
                    return;
                    }
                addToLog(description.toString(), fullInfo ? 0 : greatTextSize);
                InputStream is = getContentResolver().openInputStream(uri);
                processInputStream(is);
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
    private void showShort(){
        ArrayList<Extreme> list = inputStat.createExtrems(true,noFirstPoints,noLastPoints);
        if (list.size()==0){
            addToLog("Экстремумов не найдено",greatTextSize);
            return;
            }
        addToLog(String.format("Основная частота=%6.4f гц",list.get(0).freq/KF100),greatTextSize);
        }
    //--------------------------------------------------------------------------
    private FFTCallBack back = new FFTCallBack(){
        @Override
        public void onStart(float msOnStep) {
            }
        @Override
        public void onFinish() {
            inputStat.smooth(kSmooth);
            if (fullInfo)
                showStatistic();
            else
                showShort();
            addGraphView(inputStat,MiddleMode);
            addToLog("");
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
            if (!hideFFTOutput)
                addToLog(mes);
            }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
    //--------------------------------------------------------------------------
    public final String androidFileDirectory(){
        return getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        }
    public void loadArchive(){
        try {
            Gson gson = new Gson();
            File ff = new File(androidFileDirectory());
            if (!ff.exists()) {
                ff.mkdir();
                }
            String ss = androidFileDirectory()+"/"+archiveFile;
            InputStreamReader out = new InputStreamReader(new FileInputStream(ss), "UTF-8");
            archive = (DataDesription) gson.fromJson(out, DataDesription.class);
            out.close();
            } catch (Exception ee) {
                addToLog("Ошибка чтения архива "+ee.toString());
                addToLog("Создан пустой");
                saveArchive();
                }
            }
    public void saveArchive() {
        try {
            Gson gson = new Gson();
            File ff = new File(androidFileDirectory());
            if (!ff.exists()) {
                ff.mkdir();
                }
            String ss = androidFileDirectory()+"/"+archiveFile;
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(ss), "UTF-8");
            gson.toJson(archive, out);
            out.flush();
            out.close();
            } catch (Exception ee) {
                addToLog("Ошибка записи архива "+ee.toString()); }
        }
    //------------------------------------------------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_fullOutSide) {
            fullInfo=true;
            hideFFTOutput=false;
            preloadFromText();
            return true;
            }
        if (id == R.id.action_shortOutSide) {
            fullInfo=false;
            hideFFTOutput=true;
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
        if (id == R.id.action_dir) {
            createArchive();
            for(FileDescription ff : archive.fileList)
                addArchiveItemToLog(ff);
            return true;
            }
        return super.onOptionsItemSelected(item);
        }

    public void addArchiveItemToLog(final FileDescription ff){
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    fullInfo=false;
                    hideFFTOutput=true;
                    FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+ff.originalFileName);
                    addToLog(ff.toString(),greatTextSize);
                    processInputStream(fis);
                    } catch (Exception e) {
                        addToLog("Файл не открыт: "+ff.originalFileName+"\n"+e.toString());
                        }
                }
            };
        View.OnLongClickListener listenerLong = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    fullInfo=true;
                    hideFFTOutput=false;
                    FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+ff.originalFileName);
                    addToLog(ff.toString());
                    processInputStream(fis);
                    } catch (Exception e) {
                        addToLog("Файл не открыт: "+ff.originalFileName+"\n"+e.toString());
                        return false;
                        }
                return true;
                }
            };
        addToLogButton(ff.toString(),listener,listenerLong);
        }
    public void createArchive(){
        File ff = new File(androidFileDirectory());
        if (!ff.exists()) {
            ff.mkdir();
            }
        archive.fileList.clear();
        for(String ss : ff.list()){
            FileDescription dd = new FileDescription(ss);
            String zz = dd.parseFromName();
            if (zz!=null)
                addToLog("Файл: "+ss+" "+zz);
            else
                archive.fileList.add(dd);
        }
    }
}
