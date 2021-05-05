package me.romanow.lep500;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
    public BTViewFace btViewFace = new BTViewFace(this);
    public YandexDiskService yadisk = new YandexDiskService(this);
    public MailSender mail = new MailSender(this);
    private FFT fft = new FFT();
    private LayerStatistic inputStat = new LayerStatistic("Входные данные");
    LEP500Settings set = new LEP500Settings();
    //-------------- Постоянные параметры snn-core ---------------------------------------
    public Thread guiThead;
    private final boolean  p_Compress=false;        // Нет компрессии
    private final int  compressLevel=0;
    private final int greatTextSize=20;             // Крупный шрифт
    private final float kMultiple=3.0f;
    private final float kAmpl=1f;
    private final int KF100=FFT.sizeHZ/100;
    private final int MiddleMode=0x01;
    private final int DispMode=0x02;
    private final int MiddleColor = 0x0000FF00;
    private final int DispColor = 0x000000FF;
    private final int GraphBackColor = 0x00A0C0C0;
    final static String archiveFile="LEP500Archive.json";
    //------------------------------------------------------------------------------------
    private int nFirstMax=10;               // Количество максимумов в статистике (вывод)
    private int noFirstPoints=20;           // Отрезать точек справа и слева
    private int noLastPoints=1000;
    private boolean fullInfo=false;         // Вывод полной инофрмации о спектре
    private boolean hideFFTOutput=false;
    private DataDesription archive = new DataDesription();
    private double freqStep = 0;
    private int waveMas=1;
    private double waveStartTime=0;
    //----------------------------------------------------------------------------
    private LinearLayout log;
    private ScrollView scroll;
    private final int CHOOSE_RESULT=100;
    private final int CHOOSE_RESULT_COPY=101;
    public final int REQUEST_ENABLE_BT=102;
    public final String BT_OWN_NAME="LEP500";
    public final String BT_SENSOR_NAME_PREFIX="VIBR_SENS";
    public final int BT_DISCOVERY_TIME_IN_SEC=300;
    public final int BT_SCANNING_TIME_IN_SEC=30;
    private ImageView MenuButton;
    private ImageView GPSState;
    //--------------------------------------------------------------------------
    public void guiCall(Runnable code){
        if (Thread.currentThread()==guiThead)
            code.run();
        else
            runOnUiThread(code);
        }
    public GPSService gpsService = new GPSService(new GPSListener() {
        @Override
        public void onEvent(String ss) {
            addToLog(ss);
        }
        @Override
        public void onGPS(GPSPoint gpsPoint) {
            int state = gpsPoint.state();
            if (state == GPSPoint.GeoNone)
                GPSState.setImageResource(R.drawable.gps_off);
            if (state ==GPSPoint.GeoNet)
                GPSState.setImageResource(R.drawable.gsm);
            if (state ==GPSPoint.GeoGPS)
                GPSState.setImageResource(R.drawable.gps);
            }
        });
    private EventListener logEvent = new EventListener() {
        @Override
        public void onEvent(String ss) {
            addToLog(ss);
            }
        };

    //------------------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            guiThead = Thread.currentThread();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_main);
            btViewFace.init();
            MenuButton = (ImageView) findViewById(R.id.headerMenu);
            GPSState = (ImageView) findViewById(R.id.headerGPS);
            log = (LinearLayout) findViewById(R.id.log);
            scroll = (ScrollView)findViewById(R.id.scroll);
            loadSettings();
            } catch (Exception ee){ addToLog(createFatalMessage(ee,10));}
        // Регистрируем BroadcastReceiver
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(blueToothReceiver, filter);// Не забудьте снять регистрацию в onDestroy
        gpsService.startService(this);
        MenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ListBoxDialog(MainActivity.this, MenuItems, "Меню", new ListBoxListener() {
                    @Override
                    public void onSelect(int index) {
                        procMenuItem(index);
                    }
                    @Override
                    public void onLongSelect(int index) {}
                }).create();
            }
        });
        btViewFace.init();
        addToLog("Звенящие опоры России",25);
        }

    public void popupAndLog(String ss){
        addToLog(ss);
        popupInfo(ss);
        }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueToothReceiver);
        btViewFace.blueToothOff();
        gpsService.stopService();
        }
    public void scrollDown(){
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    public void saveBTFile(BTReceiver sensor, LEP500File file){
        file.save(androidFileDirectory(), logEvent);
        addToLog(btViewFace.getSensorName(sensor)+" записано: "+file.createOriginalFileName()+" ["+file.getData().length+"]");
        set.measureCounter++;           // Номер следующего измерения
        saveSettings();
        }
    private void calcFirstLastPoints(){
        int width=fft.getParams().W();
        double df = 100./width;
        noFirstPoints = (int)(set.FirstFreq/df);
        noLastPoints = (int)((50-set.LastFreq)/df);
        }
    public void addToLog(String ss){
        addToLog(false, ss,0);
        }
    public void addToLog(boolean fullInfoMes, String ss){
        addToLog(fullInfoMes, ss,0);
        }
    public void addToLog(String ss, int textSize){
        addToLog(false,ss,textSize);
        }
    public void addToLog(boolean fullInfoMes, final String ss, final int textSize){
        if (fullInfoMes && !set.fullInfo)
            return;
        guiCall(new Runnable() {
            @Override
            public void run() {
                TextView txt = new TextView(MainActivity.this);
                txt.setText(ss);
                if (textSize!=0)
                    txt.setTextSize(textSize);
                log.addView(txt);
                scrollDown();
            }
        });
        }
    public void addToLog(final String ss, final  int textSize, final View.OnClickListener listener){
        guiCall(new Runnable() {
            @Override
            public void run() {
                Button tt = new Button(MainActivity.this);
                tt.setText(ss);
                tt.setPadding(5,5,5,5);
                tt.setBackgroundResource(R.drawable.button_background);
                tt.setTextColor(0xFFFFFFFF);
                tt.setOnClickListener(listener);
                tt.setTextSize(textSize);
                log.addView(tt);
                scrollDown();
                }
            });
        }
    public void addToLogButton(String ss){
        addToLogButton(ss,null,null);
        }
    public void addToLogButton(String ss, View.OnClickListener listener){
        addToLogButton(ss,listener,null);
        }
    public void addToLogButton(String ss, View.OnClickListener listener, View.OnLongClickListener listenerLong){
        LinearLayout button = (LinearLayout)getLayoutInflater().inflate(R.layout.button,null);
        Button bb = (Button)button.findViewById(R.id.button_press);
        bb.setText(ss);
        bb.setTextSize(greatTextSize);
        if (listener!=null)
            bb.setOnClickListener(listener);
        if (listenerLong!=null)
            bb.setOnLongClickListener(listenerLong);
        log.addView(button);
        scrollDown();
        }

    private void preloadFromText(int resultCode){
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("text/plain");
        intent = Intent.createChooser(chooseFile, "Выбрать txt");
        startActivityForResult(intent, resultCode);
        }


    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    }
                } finally { cursor.close(); }
            }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1){
                result = result.substring(cut + 1);
                }
            }
        return result;
        }

    public void processInputStream(InputStream is) throws Throwable{
        FFTAudioTextFile xx = new FFTAudioTextFile();
        xx.setnPoints(set.nTrendPoints);
        xx.readData(new BufferedReader(new InputStreamReader(is, "Windows-1251")));
        xx.removeTrend(set.nTrendPoints);
        long lnt = xx.getFrameLength();
        //for(p_BlockSize=1;p_BlockSize*FFT.Size0<=lnt;p_BlockSize*=2);
        //if (p_BlockSize!=1) p_BlockSize/=2;
        FFTParams params = new FFTParams().W(set.p_BlockSize*FFT.Size0).procOver(set.p_OverProc).
                FFTWindowReduce(false).p_Cohleogram(false).p_GPU(false).compressMode(false).
                winMode(set.winFun);
        fft.setFFTParams(params);
        fft.calcFFTParams();
        freqStep = fft.getStepHZLinear()/KF100;
        if (!hideFFTOutput){
            addToLog("Отсчетов "+xx.getFrameLength());
            addToLog("Кадр: "+set.p_BlockSize*FFT.Size0);
            addToLog("Перекрытие: "+set.p_OverProc);
            addToLog("Дискретность: "+String.format("%5.4f",freqStep)+" гц");
            }
        inputStat.reset();
        fft.fftDirect(xx,back);
        }

    public Pair<InputStream, FileDescription> openSelected(Intent data) throws FileNotFoundException {
        Uri uri = data.getData();
        String ss = getFileName(uri);
        /*
        String ss = uri.getEncodedPath();
        try {
            ss = URLDecoder.decode( ss, "UTF-8" );
            } catch (UnsupportedEncodingException e) {
                addToLog("Системная ошибка в имени файла:"+e.toString());
                addToLog(ss);
                return new Pair<>(null,null);
                }
        String ss0 = ss;
        int idx= ss.lastIndexOf("/");
        if (idx!=-1) ss = ss.substring(idx+1);
         */
        FileDescription description = new FileDescription(ss);
        String out = description.parseFromName();
        if (out!=null){
            addToLog("Имя файла: "+out);
            return new Pair(null,null);
            }
        addToLog(description.toString(), fullInfo ? 0 : greatTextSize);
        InputStream is = getContentResolver().openInputStream(uri);
        return new Pair(is,description);
        }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        String path     = "";
        try {
            if(requestCode == REQUEST_ENABLE_BT) {
                popupAndLog("BlueTooth включен, повторите команду");
                }
            if(requestCode == CHOOSE_RESULT) {
                InputStream is = openSelected(data).o1;
                if (is==null)
                    return;
                processInputStream(is);
                }
            if(requestCode == CHOOSE_RESULT_COPY) {
                final Pair<InputStream, FileDescription> pp = openSelected(data);
                final InputStream is = pp.o1;
                if (is==null)
                    return;
                File ff = new File(androidFileDirectory());
                if (!ff.exists()) {
                    ff.mkdir();
                    }
                final FileOutputStream fos = new FileOutputStream(androidFileDirectory()+"/"+pp.o2.originalFileName);
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (true) {
                                int vv = is.read();
                                if (vv == -1)
                                    break;
                                fos.write(vv);
                            }
                            fos.flush();
                            fos.close();
                            is.close();
                            } catch (final Exception ee) {
                                addToLog(createFatalMessage(ee,10));
                                }
                        }});
                    thread.start();
                    }
            } catch (Throwable ee){
                addToLog(createFatalMessage(ee,10));
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
        addToLog(String.format("Диапазон экстремумов: %6.4f-%6.4f",50./sz*noFirstPoints,50./sz*(sz-noLastPoints)));
        ArrayList<Extreme> list = inputStat.createExtrems(mode,noFirstPoints,noLastPoints,true);
        if (list.size()==0){
            addToLog("Экстремумов не найдено");
            return;
            }
        int count = nFirstMax < list.size() ? nFirstMax : list.size();
        Extreme extreme = list.get(0);
        double val0 = mode ? extreme.value : extreme.diff;
        addToLog(mode ? "По амплитуде" : "По спаду");
        addToLog(String.format("Ампл=%6.4f Пик=%6.4f f=%6.4f гц",extreme.value,extreme.diff,extreme.idx*freqStep));
        double sum=0;
        for(int i=1; i<count;i++){
            extreme = list.get(i);
            double proc = (mode ? extreme.value : extreme.diff)*100/val0;
            sum+=proc;
            addToLog(String.format("Ампл=%6.4f Пик=%6.4f f=%6.4f гц %d%%",extreme.value,extreme.diff, extreme.idx*freqStep,(int)proc));
            }
        addToLog(String.format("Средний - %d%% к первому",(int)(sum/(count-1))));
        }

    private void showStatistic(){
        showExtrems(true);
        showExtrems(false);
        }
    private void showShort(){
        ArrayList<Extreme> list = inputStat.createExtrems(true,noFirstPoints,noLastPoints,true);
        if (list.size()==0){
            addToLog("Экстремумов не найдено",greatTextSize);
            return;
            }
        addToLog(String.format("Основная частота=%6.4f гц",list.get(0).idx*freqStep),greatTextSize);
        }
    //--------------------------------------------------------------------------
    private FFTCallBack back = new FFTCallBack(){
        @Override
        public void onStart(float msOnStep) {
            }
        @Override
        public void onFinish() {
            calcFirstLastPoints();
            if (inputStat.getCount()==0){
                popupAndLog("Настройки: короткий период измерений/много блоков");
                return;
                }
            inputStat.smooth(set.kSmooth);
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

    //@Override
    //public boolean onCreateOptionsMenu(Menu menu) {
    //    // Inflate the menu; this adds items to the action bar if it is present.
    //    getMenuInflater().inflate(R.menu.menu_main, menu);
    //    return true;
    //}

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
        //toastContainer.setBackgroundResource(R.color.status_almostFree);
        toast3.setGravity(Gravity.TOP, 0, 20);
        toast3.show();
        }
    public void popupInfo(final String ss) {
        guiCall(new Runnable() {
            @Override
            public void run() {
                popupToast(R.drawable.info,ss);
            }
        });
        }
    //--------------------------------------------------------------------------
    public void paintOne(LineGraphView graphView,float data[], int color,int noFirst,int noLast,boolean freqMode){
        GraphView.GraphViewData zz[] = new GraphView.GraphViewData[data.length-noFirst-noLast];
        for(int j=noFirst;j<data.length-noLast;j++){                    // Подпись значений факторов j-ой ячейки
            double freq = freqMode ? (j*50./data.length) : (j/100.);
            zz[j-noFirst] = new GraphView.GraphViewData(freq,data[j]);
            }
        GraphViewSeries series = new GraphViewSeries(zz);
        series.getStyle().color = color | 0xFF000000;
        graphView.addSeries(series);
        }

    private void addGraphView(LayerStatistic stat, int mode){
        LinearLayout lrr=(LinearLayout)getLayoutInflater().inflate(R.layout.graphview, null);
        LinearLayout panel = (LinearLayout)lrr.findViewById(R.id.viewPanel);
        LineGraphView graphView = new LineGraphView(this,"");
        graphView.setScalable(true);
        graphView.setScrollable(true);
        graphView.getGraphViewStyle().setTextSize(15);
        panel.addView(graphView);
        log.addView(lrr);
        if ((mode & MiddleMode)!=0)
            paintOne(graphView,inputStat.getMids(),MiddleColor,noFirstPoints,noFirstPoints,true);
        if ((mode & DispMode)!=0)
            paintOne(graphView,inputStat.getDisps(),DispColor,noFirstPoints,noFirstPoints,true);
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
                popupInfo("Ошибка чтения архива,создан пустой");
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
                popupInfo("Ошибка записи архива");
                }
    public void saveSettings() {
        try {
            Gson gson = new Gson();
            File ff = new File(androidFileDirectory());
            if (!ff.exists()) {
                ff.mkdir();
                }
            String ss = androidFileDirectory()+"/"+LEP500Settings.class.getSimpleName()+".json";
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(ss), "UTF-8");
            gson.toJson(set, out);
            out.flush();
            out.close();
        } catch (Exception ee) {
            addToLog("Ошибка записи настроек " + ee.toString());
            popupInfo("Ошибка записи настроек");
            }
        }
    public void loadSettings() {
        try {
            Gson gson = new Gson();
            File ff = new File(androidFileDirectory());
            if (!ff.exists()) {
                ff.mkdir();
                }
            String ss = androidFileDirectory()+"/"+LEP500Settings.class.getSimpleName()+".json";
            InputStreamReader out = new InputStreamReader(new FileInputStream(ss), "UTF-8");
            set = (LEP500Settings) gson.fromJson(out, LEP500Settings.class);
            out.close();
            set.createMaps();
        } catch (Exception ee) {
            popupAndLog("Ошибка чтения настроек (сброшены)");
            saveSettings();
            }
    }
    //------------------------------------------------------------------------
    private String[] MenuItems = {
            "Архив",
            "Архив подробно",
            "Файл в архив",
            "Файл кратко",
            "Файл подробно",
            "Удалить из архива",
            "Очистить ленту",
            "Настройки",
            "Измерение",
            "Образец",
            "Конвертировать в wave",
            "Список сенсоров",
            "Очистить список",
            "Просмотр волны",
            "Отправить из архива"
            };
    public void procMenuItem(int index) {
        switch (index){
case 0: selectFromArchive("Проcмотр архива",archiveProcView);
        break;
case 1: selectFromArchive("Проcмотр архива",archiveProcViewFull);
        break;
case 2: preloadFromText(CHOOSE_RESULT_COPY);
        break;
case 3: fullInfo=false;
        hideFFTOutput=true;
        preloadFromText(CHOOSE_RESULT);
        break;
case 4: fullInfo=true;
        hideFFTOutput=false;
        preloadFromText(CHOOSE_RESULT);
        break;
case 5: selectFromArchive("Удалить из архива",deleteSelector);
        break;
case 6: log.removeAllViews();
        break;
case 7: new SettingsMenu(this);
        break;
case 8: btViewFace.selectSensorGroup(new SensorGroupListener() {
            @Override
            public void onSensor(ArrayList<BTReceiver> receiverList) {
                for(BTReceiver receiver :  receiverList){
                    String name = btViewFace.getSensorName(receiver).replace("_","-");
                    LEP500File file = new LEP500File(set,name,gpsService.lastGPS());
                    receiver.startMeasure(file,false);
                    }
                }
            });
        break;
case 9: LEP500File file2 = new LEP500File(set,"Тест",gpsService.lastGPS());
        BTReceiver receiver = new BTReceiver(btViewFace,btViewFace.BTBack);
        receiver.startMeasure(file2,true);
        break;
case 10:selectFromArchive("Конвертировать в wave",convertSelector);
        break;
case 11:for(BTDescriptor descriptor : set.knownSensors)
            addToLog("Датчик: "+descriptor.btName+": "+descriptor.btMAC);
        break;
case 12:set.knownSensors.clear();
        set.createMaps();
        saveSettings();
        break;
case 13:showWaveForm();
        break;
case 14:selectFromArchive("Отправить Mail",sendMailSelector);
        break;
        }
    }
    //----------------------------------------------------------------------------------------------
    private  I_ArchveSelector uploadSelector = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            File file = new File(androidFileDirectory()+"/"+fd.originalFileName);
            yadisk.init();
            }
        };
    private  I_ArchveSelector deleteSelector = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            File file = new File(androidFileDirectory()+"/"+fd.originalFileName);
            file.delete();
            createArchive();
            }
        };
    private  I_ArchveSelector convertSelector = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            String pathName = androidFileDirectory()+"/"+fd.originalFileName;
            FFTAudioTextFile xx = new FFTAudioTextFile();
            xx.setnPoints(set.nTrendPoints);
            hideFFTOutput=false;
            xx.convertToWave(pathName, back);
            }
        };
    private  I_ArchveSelector sendMailSelector = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            try {
                //SendMail sm = new SendMail(MainActivity.this, fd);
                //sm.execute();
                //try{
                //    mail.sendMail(fd);
                //    } catch (Exception ee){
                //        addToLog("Ошибка mail: "+ee.toString());
                //        }
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{set.mailToSend});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Датчик: " + fd.toString());
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "");
                String filePath = androidFileDirectory() + "/" + fd.originalFileName;
                addToLog(filePath);
                File ff = new File(filePath);
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri fileUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, ff);
                emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,fileUri);
                //--------------- Старое -------------------------------------------------------
                //emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,Uri.fromFile(ff));
                //emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,Uri.parse(filePath));
                emailIntent.setType("text/text");
                startActivity(Intent.createChooser(emailIntent, "Отправка письма..."));
                } catch (Exception ee){
                    addToLog("Ошибка mail: "+ee.toString());
                    }
            }
        };
    //----------------------------------------------------------------------------------------------
    private void procArchive(FileDescription fd, boolean longClick){
        String fname = fd.originalFileName;
        try {
            fullInfo=longClick;
            hideFFTOutput=!longClick;
            FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+fname);
            addToLog(fd.toString(),greatTextSize);
            processInputStream(fis);
            } catch (Throwable e) {
                addToLog("Файл не открыт: "+fname+"\n"+createFatalMessage(e,10));
                }
        }
    private I_ArchveSelector archiveProcView = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            procArchive(fd,false);
            }
        };
    private I_ArchveSelector archiveProcViewFull = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            procArchive(fd,true);
        }
    };
    //----------------------------------------------------------------------------------------------
    View.OnClickListener waveStartEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new OneParameterDialog(MainActivity.this, "Параметр графика", "Начало (сек)", "" + waveStartTime, false, false,new EventListener() {
                @Override
                public void onEvent(String ss) {
                    try {
                        double val = Double.parseDouble(ss);
                        if (val<0 || val>currentWave.getData().length/100.){
                            popupAndLog("Выход за пределы диапазона");
                            return;
                            }
                        waveStartTime = val;
                        procWaveForm();
                        } catch (Exception ee){
                            popupAndLog("Формат вещественного числа");
                            }
                        }
                    });
                }
            };
    //----------------------------------------------------------------------------------------------
    View.OnClickListener waveMasEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new OneParameterDialog(MainActivity.this, "Параметр графика", "Масштаб", "" + waveMas, false, false,new EventListener() {
                @Override
                public void onEvent(String ss) {
                    try {
                        int val = Integer.parseInt(ss);
                        waveMas = val;
                        procWaveForm();
                    } catch (Exception ee){
                        popupAndLog("Формат целого числа");
                        }
                }
            });
        }
    };
    //----------------------------------------------------------------------------------------------
    private FFTAudioTextFile currentWave;
    public  void procWaveForm(final FFTAudioTextFile xx){
        currentWave = xx;
        procWaveForm();
        }
    public  void procWaveForm(){
        LinearLayout lrr=(LinearLayout)getLayoutInflater().inflate(R.layout.graphview, null);
        LinearLayout panel = (LinearLayout)lrr.findViewById(R.id.viewPanel);
        LineGraphView graphView = new LineGraphView(this,"");
        graphView.setScalable(true);
        graphView.setScrollable(true);
        graphView.getGraphViewStyle().setTextSize(15);
        panel.addView(graphView);
        addToLog("Начало (сек)",20,waveStartEvent);
        addToLog("Масштаб",20,waveMasEvent);
        log.addView(lrr);
        int firstPoint = (int)(waveStartTime*100);
        int size = currentWave.getData().length;
        int count = size/waveMas;
        int lastPoint = size - firstPoint - count;
        if (lastPoint<0) lastPoint=0;
        paintOne(graphView,currentWave.getData(),DispColor,firstPoint,lastPoint,false);
        addToLog("");
        }

    public  void procWaveForm(int index){
        FileDescription fd = archive.fileList.get(index);
        String fname = fd.originalFileName;
        try {
            FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+fname);
            addToLog(fd.toString(),greatTextSize);
            FFTAudioTextFile xx = new FFTAudioTextFile();
            xx.readData(new BufferedReader(new InputStreamReader(fis, "Windows-1251")));
            waveMas=1;
            waveStartTime=0;
            procWaveForm(xx);
            } catch (Throwable e) {
                addToLog("Файл не открыт: "+fname+"\n"+createFatalMessage(e,10));
                }
        }

    public void selectFromArchive(String title, final I_ArchveSelector selector){
        createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : archive.fileList)
            out.add(ff.toString());
        new ListBoxDialog(this, out, title, new ListBoxListener() {
            @Override
            public void onSelect(int index) {
                selector.onSelect(archive.fileList.get(index),false);
                }
            @Override
            public void onLongSelect(int index) {
                selector.onSelect(archive.fileList.get(index),true);
                }
            }).create();
        }

    public void showWaveForm(){
        createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : archive.fileList)
            out.add(ff.toString());
        new ListBoxDialog(this, out, "Просмотр волны", new ListBoxListener() {
            @Override
            public void onSelect(int index) {
                procWaveForm(index);
                }
            @Override
            public void onLongSelect(int index) {}
            }).create();
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
                    } catch (Throwable e) {
                        addToLog("Файл не открыт: "+ff.originalFileName+"\n"+createFatalMessage(e,10));
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
                    } catch (Throwable e) {
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
            if (!dd.originalFileName.toUpperCase().endsWith(".TXT"))
                continue;
            String zz = dd.parseFromName();
            if (zz!=null)
                addToLog("Файл: "+ss+" "+zz);
            else
                archive.fileList.add(dd);
        }
    }
    //----------------------------------------------------------------------------------------------
    // Создаем BroadcastReceiver для ACTION_FOUND
    private final BroadcastReceiver blueToothReceiver=new BroadcastReceiver(){
        public void onReceive(Context context, Intent intent){
            String action= intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device= intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                addToLog("BlueTooth: "+device.getName()+" "+device.getAddress());
                if (device.getName().startsWith(BT_SENSOR_NAME_PREFIX)){
                    BTReceiver receiver = new BTReceiver(btViewFace,btViewFace.BTBack);
                    receiver.blueToothOn(device);
                    btViewFace.sensorList.add(receiver);
                    }
                }
            }
        };
    private void addSensorName(){
        if (btViewFace.sensorList.size()!=1){
            popupAndLog("Нужен один активный датчик");
            return;
            }
        final BTReceiver receiver = btViewFace.sensorList.get(0);
        if (set.addressMap.get(receiver.getSensorMAC())!=null){
            popupAndLog("Датчик с именем: "+btViewFace.getSensorName(receiver));
            return;
            }
        new OneParameterDialog(this, "Имя датчика",receiver.getSensorMAC(),"", false, true,new EventListener() {
            @Override
            public void onEvent(String ss) {
                if (set.nameMap.get(ss)!=null){
                    popupAndLog("Имя используется: "+ss);
                    return;
                    }
                set.knownSensors.add(new BTDescriptor(ss,receiver.getSensorMAC()));
                set.createMaps();
                saveSettings();
                }
            });
        }

}
