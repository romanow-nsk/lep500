package me.romanow.lep500;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
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

import android.os.Handler;
import android.os.ParcelUuid;
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
import java.util.List;
import java.util.UUID;

import romanow.snn_simulator.fft.FFT;
import romanow.snn_simulator.fft.FFTAudioTextFile;
import romanow.snn_simulator.fft.FFTCallBack;
import romanow.snn_simulator.fft.FFTParams;
import romanow.snn_simulator.layer.Extreme;
import romanow.snn_simulator.layer.LayerStatistic;

public class MainActivity extends AppCompatActivity {
    private FFT fft = new FFT();
    private LayerStatistic inputStat = new LayerStatistic("Входные данные");
    LEP500Settings set = new LEP500Settings();
    //-------------- Постоянные параметры snn-core ---------------------------------------
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
    public final int SensorMaxNumber=4;
    private int nFirstMax=10;               // Количество максимумов в статистике (вывод)
    private int noFirstPoints=20;           // Отрезать точек справа и слева
    private int noLastPoints=1000;
    private boolean fullInfo=false;
    private boolean hideFFTOutput=false;
    private DataDesription archive = new DataDesription();
    private double freqStep = 0;
    //----------------------------------------------------------------------------
    private LinearLayout log;
    private ScrollView scroll;
    private final int CHOOSE_RESULT=100;
    private final int CHOOSE_RESULT_COPY=101;
    private final int REQUEST_ENABLE_BT=102;
    private final String BT_OWN_NAME="LEP500";
    private final String BT_SENSOR_NAME_PREFIX="VIBR_SENS";
    private final int BT_DISCOVERY_TIME_IN_SEC=300;
    private final int BT_SCANNING_TIME_IN_SEC=30;
    private BluetoothLeScanner scanner = null;
    private ImageView BTState[]=new ImageView[4];
    private TextView BTStateText[]=new TextView[4];
    private ImageView BTScanerState;
    private ImageView MenuButton;
    private ImageView GPSState;
    private ArrayList<BTReceiver> sensorList = new ArrayList<>();
    //--------------------------------------------------------------------------
    private GPSService gpsService = new GPSService(new GPSListener() {
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
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_main);
            //Toolbar toolbar = findViewById(R.id.toolbar);
            //setSupportActionBar(toolbar);
            MenuButton = (ImageView) findViewById(R.id.headerMenu);
            BTState[0] = (ImageView) findViewById(R.id.headerState0);
            BTState[1] = (ImageView) findViewById(R.id.headerState1);
            BTState[2] = (ImageView) findViewById(R.id.headerState2);
            BTState[3] = (ImageView) findViewById(R.id.headerState3);
            BTScanerState  = (ImageView) findViewById(R.id.headerScanerState);
            BTStateText[0] = (TextView) findViewById(R.id.headerStateText0);
            BTStateText[1] = (TextView) findViewById(R.id.headerStateText1);
            BTStateText[2] = (TextView) findViewById(R.id.headerStateText2);
            BTStateText[3] = (TextView) findViewById(R.id.headerStateText3);
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
                    public void onLongSelect(int index) {

                    }
                }).create();
            }
        });
        addToLog("Звенящие опоры России",25);
        //blueToothOn();            // Для отладки
        }
    private void blueToothOff(){
        for(BTReceiver receiver : sensorList)
            receiver.blueToothOff();
        sensorList.clear();
        setBTScanerState(BT_Gray);
        }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(blueToothReceiver);
        blueToothOff();
        gpsService.stopService();
        }
    private BTListener btBack = new BTListener() {
        @Override
        public void notify(BTReceiver sensor, String ss) {
            addToLog(ss);
            }
        @Override
        public void onReceive(BTReceiver sensor, LEP500File file){
            file.save(androidFileDirectory(), logEvent);
            addToLog("Записано: "+file.createOriginalFileName()+" ["+file.getData().length+"]");
            set.measureCounter++;           // Номер следующего измерения
            saveSettings();
            }
        @Override
        public void onState(BTReceiver sensor, int state) {
            setBTState(sensor,state);
            }

        @Override
        public void onStateText(BTReceiver sensor, String text) {
            setBTStateText(sensor,text);
            }
    };
    public void scrollDown(){
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

    private void calcFirstLastPoints(){
        int width=fft.getParams().W();
        double df = 100./width;
        noFirstPoints = (int)(set.FirstFreq/df);
        noLastPoints = (int)((50-set.LastFreq)/df);
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
        scrollDown();
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
                addToLog("BlueTooth включен, повторите команду");
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addToLog(createFatalMessage(ee,10));
                                    }
                                });
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
                addToLog("Настройки: короткий период измерений/много блоков");
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
            addToLog("Ошибка записи настроек "+ee.toString()); }
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
        } catch (Exception ee) {
            addToLog("Ошибка чтения настроек (сброшены)");
            saveSettings();
            }
    }
    //------------------------------------------------------------------------
    private String[] MenuItems = {
            "Архив",
            "Файл в архив",
            "Файл кратко",
            "Файл подробно",
            "Удалить из архива",
            "Очистить ленту",
            "Настройки",
            "Сканер-старт",
            "Сканер-стоп",
            "Выключить датчик",
            "Измерение",
            "Прервать",
            "Образец",
            "Уровень заряда",
            "Конвертировать в wave"
            };
    public void procMenuItem(int index) {
        switch (index){
case 0:
        selectFromArchive();
        break;
case 1: preloadFromText(CHOOSE_RESULT_COPY);
        break;
case 2: fullInfo=false;
        hideFFTOutput=true;
        preloadFromText(CHOOSE_RESULT);
        break;
case 3: fullInfo=true;
        hideFFTOutput=false;
        preloadFromText(CHOOSE_RESULT);
        break;
case 4: deleteDialog();
        break;
case 5: log.removeAllViews();
        break;
case 6: new SettingsMenu(this);
        break;
case 7: blueToothOn();
        break;
case 8:
        if (scanner!=null)
            scanner.stopScan(scanCallback);
        setBTScanerState(BT_Gray);
        break;
case 9: selectSensor(new SensorListener() {
            @Override
            public void omSensor(BTReceiver receiver) {
                receiver.deviceOff();
                receiver.blueToothOff();
                }
            });
        break;
case 10:selectSensor(new SensorListener() {
            @Override
            public void omSensor(BTReceiver receiver) {
                String name = receiver.getSensorName().replace("_","-");
                LEP500File file = new LEP500File(set,name,gpsService.lastGPS());
                receiver.startMeasure(file,false);
                }
            });
        break;
case 11:selectSensor(new SensorListener() {
            @Override
            public void omSensor(BTReceiver receiver) {
                receiver.stopMeasure();
                }
            });
        break;
case 12:LEP500File file2 = new LEP500File(set,"Тест",gpsService.lastGPS());
        BTReceiver receiver = new BTReceiver(this,btBack);
        receiver.startMeasure(file2,true);
        break;
case 13:selectSensor(new SensorListener() {
            @Override
            public void omSensor(BTReceiver receiver) {
                receiver.getChargeLevel();
                }
            });
        break;
case 14:convertDialog();
        break;
        }
    }
    //----------------------------------------------------------------------------------------------
    public void deleteDialog(){
        createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : archive.fileList)
            out.add(ff.toString());
        new ListBoxDialog(this, out, "Удалить из архива", new ListBoxListener() {
            @Override
            public void onSelect(int index) {
                File file = new File(androidFileDirectory()+"/"+archive.fileList.get(index).originalFileName);
                file.delete();
                createArchive();
                }
            @Override
            public void onLongSelect(int index) {}
            }).create();
        }
    public void convertDialog(){
        createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : archive.fileList)
            out.add(ff.toString());
        new ListBoxDialog(this, out, "Конвертировать в wave", new ListBoxListener() {
            @Override
            public void onSelect(int index) {
                String pathName = androidFileDirectory()+"/"+archive.fileList.get(index).originalFileName;
                FFTAudioTextFile xx = new FFTAudioTextFile();
                xx.setnPoints(set.nTrendPoints);
                hideFFTOutput=false;
                xx.convertToWave(pathName, back);
                }
            @Override
            public void onLongSelect(int index) {}
        }).create();
    }


    public  void procOnClick(int index,boolean longClick){
        FileDescription fd = archive.fileList.get(index);
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

    public void selectFromArchive(){
        createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : archive.fileList)
            out.add(ff.toString());
        new ListBoxDialog(this, out, "Промотр архива", new ListBoxListener() {
            @Override
            public void onSelect(int index) {
                procOnClick(index,false);
                }
            @Override
            public void onLongSelect(int index) {
                procOnClick(index,true);
                }
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
                    BTReceiver receiver = new BTReceiver(MainActivity.this,btBack);
                    receiver.blueToothOn(device);
                    sensorList.add(receiver);
                    }
                }
            }
    };
    //----------------------------------------------------------------------------------------------
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device.getName()==null)
                return;
            addToLog("BlueTooth: "+device.getName()+" "+device.getAddress());
            if (device.getName().startsWith(BT_SENSOR_NAME_PREFIX)){
                addToLog("BlueTooth: "+device.getName()+" подключение");
                scanner.stopScan(scanCallback);
                scannerHandler.removeCallbacks(scanerTimeOut);
                BTReceiver receiver = new BTReceiver(MainActivity.this,btBack);
                receiver.blueToothOn(device);
                sensorList.add(receiver);
                }
            }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {}
        @Override
        public void onScanFailed(int errorCode) {}
        };
    //----------------------------------------------------------------------------------------------
    Handler scannerHandler = new Handler();
    Runnable scanerTimeOut = new Runnable() {
        @Override
        public void run() {
            addToLog("Тайм-аут сканирования");
            if (scanner!=null)
                scanner.stopScan(scanCallback);
            setBTScanerState(BT_Gray);
        }
    };
    //----------------------------------------------------------------------------------------------
    public final static int BT_Gray=0;
    public final static int BT_Red=1;
    public final static int BT_Yellow=2;
    public final static int BT_Green=3;
    public final static int BT_LightRed=4;
    public final static int BT_LightGreen=5;
    private final static int BTStateID[]={R.drawable.status_gray,R.drawable.status_red,R.drawable.status_yellow,
            R.drawable.status_green,R.drawable.status_light_red,R.drawable.status_light_green};
    private void setBTScanerState(int state){
        BTScanerState.setImageResource(BTStateID[state]);
        }
    private void setBTState(BTReceiver receiver,int state){
        int idx = sensorList.indexOf(receiver);
        if (idx!=-1 && idx < SensorMaxNumber)
            BTState[idx].setImageResource(BTStateID[state]);
        }
    private void setBTStateText(BTReceiver receiver,String text){
        int idx = sensorList.indexOf(receiver);
        if (idx!=-1 && idx < SensorMaxNumber)
            BTStateText[idx].setText(text);
    }
    private void setBTState(ImageView view,int state){
        view.setImageResource(BTStateID[state]);
        }
    public void blueToothOn(){
        blueToothOff();
        BluetoothAdapter bluetooth= BluetoothAdapter.getDefaultAdapter();
        if(bluetooth==null) {
            addToLog("Нет модуля BlueTooth");
            setBTScanerState(BT_Red);
            return;
            }
        if (!bluetooth.isEnabled()) {
            // Bluetooth выключен. Предложим пользователю включить его.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
            }
        bluetooth.setName(BT_OWN_NAME);
        int btState= bluetooth.getState();
        if (btState==BluetoothAdapter.STATE_ON){
            addToLog("Состояние BlueTooth: включен");
            setBTScanerState(BT_Green);
            }
        if (btState==BluetoothAdapter.STATE_TURNING_ON){
            addToLog("Состояние BlueTooth: включается");
            setBTScanerState(BT_Yellow);
            }
        if (btState==BluetoothAdapter.STATE_OFF){
            addToLog("Состояние BlueTooth: выключен");
            setBTScanerState(BT_Red);
            }
        if (btState==BluetoothAdapter.STATE_TURNING_OFF){
            addToLog("Состояние BlueTooth: выключается");
            setBTScanerState(BT_Yellow);
            }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        scanner = adapter.getBluetoothLeScanner();
        if (scanner != null) {
            sensorList.clear();
            List<ScanFilter> filters = new ArrayList<>();
            int filterMode=0;
            // ----------------- Сканирование по UUID
            if (filterMode==1){
                UUID BLP_SERVICE_UUID = UUID.fromString(BTReceiver.UUID_SERVICE_STR);
                ScanFilter filter = new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(BLP_SERVICE_UUID)).build();
                filters.add(filter);
                }
            // --------------- Сканирование по имени
            if (filterMode==2) {
                String[] names = new String[]{BT_SENSOR_NAME_PREFIX};
                for (String name : names) {
                    ScanFilter filter = new ScanFilter.Builder().setDeviceName(name).build();
                    filters.add(filter);
                    }
                }
            ScanSettings scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .setReportDelay(0L)
                    .build();
            scanner.startScan(filters, scanSettings, scanCallback);
            addToLog("Сканирование началось");
            scannerHandler.postDelayed(scanerTimeOut,BT_SCANNING_TIME_IN_SEC*1000);
            }
        else{
            addToLog("Сканер BleuTooth не получен");
            setBTScanerState(BT_Red);
            return;
            }
        }
    private void selectSensor(final SensorListener listener){
        if (sensorList.size()==0){
            addToLog("Нет включенных датчиков");
            return;
            }
        ArrayList<String> sensorNames = new ArrayList<>();
        for(BTReceiver receiver : sensorList)
            sensorNames.add(receiver.getSensorName());
            new ListBoxDialog(this, sensorNames, "Датчик", new ListBoxListener() {
                @Override
                public void onSelect(int index) {
                    listener.omSensor(sensorList.get(index));
                    }
                @Override
                public void onLongSelect(int index) { }
                }
            ).create();
        }
}
