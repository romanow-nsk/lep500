package me.romanow.lep500;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.gson.Gson;
import com.jjoe64.graphview.LineGraphView;
import static androidx.core.content.ContextCompat.checkSelfPermission;


import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;

import romanow.snn_simulator.fft.FFT;
import romanow.snn_simulator.fft.FFTAudioTextFile;
import romanow.snn_simulator.layer.Extreme;
import romanow.snn_simulator.layer.LayerStatistic;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public class MainActivity extends BaseActivity {     //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public BTViewFace btViewFace = new BTViewFace(this);
    public YandexDiskService yadisk = new YandexDiskService(this);
    public MailSender mail = new MailSender(this);
    LEP500Settings set = new LEP500Settings();
    //-------------- Постоянные параметры snn-core ---------------------------------------
    public Thread guiThead;
    private final boolean  p_Compress=false;        // Нет компрессии
    private final int  compressLevel=0;
    private final float kMultiple=3.0f;
    private final float kAmpl=1f;
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
    public  boolean fullInfo=false;         // Вывод полной информации о спектре
    public boolean hideFFTOutput=false;
    private int waveMas=1;
    private double waveStartTime=0;
    //----------------------------------------------------------------------------
    private LinearLayout log;
    private ScrollView scroll;
    private final int CHOOSE_RESULT=100;
    private final int CHOOSE_RESULT_COPY=101;
    public final int REQUEST_ENABLE_BT=102;
    public final int REQUEST_ENABLE_GPS=103;
    public final String BT_OWN_NAME="LEP500";
    public final String BT_SENSOR_NAME_PREFIX="VIBR_SENS";
    public final int BT_DISCOVERY_TIME_IN_SEC=300;
    public final int BT_SCANNING_TIME_IN_SEC=60;
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
    private I_EventListener logEvent = new I_EventListener() {
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
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
                }

            new FFT();                          // статические данные
            createMenuList();
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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ENABLE_GPS);
            }
        else
            gpsService.startService(this);
        MenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ListBoxDialog(MainActivity.this, createMenuTitles(), "Меню", new I_ListBoxListener() {
                    @Override
                    public void onSelect(int index) {
                        procMenuItem(index);
                    }
                    @Override
                    public void onLongSelect(int index) {
                        int vv=1;
                    }
                }).create();
            }
        });
        btViewFace.init();
        //------------------------------------------------
        int[] surrogates = {0xD83D, 0xDC7D};
        String title = "Звенящие опоры России "+
                new String(Character.toChars(0x1F349))+
                new String(surrogates, 0, surrogates.length)+
                "\uD83D\uDC7D";
        addToLog(title,20);
        }
    public void clearLog(){
        log.removeAllViews();
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
        int width=set.p_BlockSize*FFT.Size0;
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
    @Override
    public void addToLog(String ss, int textSize){
        addToLog(false,ss,textSize);
        }

    @Override
    public void addToLogHide(String ss) {
        if (!hideFFTOutput)
            addToLog(ss);
        }

    @Override
    public void showStatisticFull(LayerStatistic inputStat) {
        if (fullInfo)
            showStatistic(inputStat);
        else
            showShort(inputStat);
        }

    public void addToLog(boolean fullInfoMes, final String ss, final int textSize){
        addToLog(fullInfoMes,ss,textSize,0);
        }
    public void addToLog(boolean fullInfoMes, final String ss, final int textSize, final int textColor){
        if (fullInfoMes && !set.fullInfo)
            return;
        guiCall(new Runnable() {
            @Override
            public void run() {
                TextView txt = new TextView(MainActivity.this);
                txt.setText(ss);
                txt.setTextColor(textColor | 0xFF000000);
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


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode==REQUEST_ENABLE_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gpsService.startService(this);
                }
            else {
                addToLog("Геолокация не разрешена");
               }
            }
        }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        String path     = "";
        try {
            if(requestCode == REQUEST_ENABLE_BT) {
                popupAndLog("BlueTooth включен, повторите команду");
                }
            if(requestCode == CHOOSE_RESULT) {
                Pair<InputStream, FileDescription> res = openSelected(data);
                InputStream is = res.o1;
                if (is==null)
                    return;
                processInputStream(is,res.o2.toString());
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

    public void procArchive(FileDescription fd, boolean longClick) {
        fullInfo = longClick;
        hideFFTOutput = !longClick;
        procArchive(fd);
        }

    private void showExtrems(LayerStatistic inputStat, boolean mode){
        int sz = inputStat.getMids().length;
        addToLog(String.format("Диапазон экстремумов: %6.4f-%6.4f",50./sz*noFirstPoints,50./sz*(sz-noLastPoints)));
        ArrayList<Extreme> list = inputStat.createExtrems(mode,noFirstPoints,noLastPoints,true);
        if (list.size()==0){
            addToLog("Экстремумов не найдено");
            return;
            }
        if (mode)
            addToLog(false,String.format("Основная частота=%6.4f гц",list.get(0).idx*freqStep),greatTextSize,paintColors[colorNum]);
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

    public synchronized void showStatistic(LayerStatistic inputStat){
        showExtrems(inputStat, true);
        showExtrems(inputStat, false);
        }
    public synchronized void showShort(LayerStatistic inputStat){
        ArrayList<Extreme> list = inputStat.createExtrems(true,noFirstPoints,noLastPoints,true);
        if (list.size()==0){
            addToLog("Экстремумов не найдено",greatTextSize);
            return;
            }
        addToLog(false,String.format("Основная частота=%6.4f гц",list.get(0).idx*freqStep),greatTextSize,paintColors[colorNum]);
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
    public DataDescription loadArchive(){
        try {
            Gson gson = new Gson();
            File ff = new File(androidFileDirectory());
            if (!ff.exists()) {
                ff.mkdir();
                }
            String ss = androidFileDirectory()+"/"+archiveFile;
            InputStreamReader out = new InputStreamReader(new FileInputStream(ss), "UTF-8");
            DataDescription archive = (DataDescription) gson.fromJson(out, DataDescription.class);
            out.close();
            return archive;
            } catch (Exception ee) {
                addToLog("Ошибка чтения архива "+ee.toString());
                addToLog("Создан пустой");
                popupInfo("Ошибка чтения архива,создан пустой");
            DataDescription archive2 = new DataDescription();
                saveArchive(archive2);
                return archive2;
                }
            }
    public void saveArchive(DataDescription archive) {
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
            AppData.ctx().set(set);
        } catch (Exception ee) {
            popupAndLog("Ошибка чтения настроек (сброшены)");
            saveSettings();
            AppData.ctx().set(set);
            }
        }
    //------------------------------------------------------------------------
    private ArrayList<MenuItemAction> menuList = new ArrayList<>();
    private String[] createMenuTitles(){
        String out[] = new String[menuList.size()];
        for(int i=0;i<out.length;i++)
            out[i]=menuList.get(i).title;
        return out;
        }
    public void procMenuItem(int index) {
        menuList.get(index).onSelect();
        }
    public void createMenuList(){
        menuList.add(new MenuItemAction("Архив") {
            @Override
            public void onSelect() {
                calcFirstLastPoints();
                selectMultiFromArchive("Проcмотр архива",procViewMultiSelector);
                }
            });
        menuList.add(new MenuItemAction("Архив подробно") {
            @Override
            public void onSelect() {
                calcFirstLastPoints();
                selectMultiFromArchive("Проcмотр архива",procViewMultiSelectorFull);
                }
            });
        menuList.add(new MenuItemAction("Полный экран") {
            @Override
            public void onSelect() {
                calcFirstLastPoints();
                selectMultiFromArchive("Полный экран",procViewSelectorFull);
                }
            });
        menuList.add(new MenuItemAction("Группировать") {
            @Override
            public void onSelect() {
                selectMultiFromArchive("Группировать",toGroupSelector);
                }
            });
        menuList.add(new MenuItemAction("Разгруппировать") {
            @Override
            public void onSelect() {
                selectMultiFromArchive(true,"Разгруппировать",fromGroupSelector);
                }
            });
        menuList.add(new MenuItemAction("Очистить ленту") {
            @Override
            public void onSelect() {
                log.removeAllViews();
                }
            });
        menuList.add(new MenuItemAction("Удалить из архива") {
            @Override
            public void onSelect() {
                selectMultiFromArchive("Удалить из архива",deleteMultiSelector);
                }
            });
        menuList.add(new MenuItemAction("Измерение") {
            @Override
            public void onSelect() {
                btViewFace.selectSensorGroup(new SensorGroupListener() {
                    @Override
                    public void onSensor(ArrayList<BTReceiver> receiverList) {
                        for(BTReceiver receiver :  receiverList){
                            String name = btViewFace.getSensorName(receiver).replace("_","-");
                            LEP500File file = new LEP500File(set,name,gpsService.lastGPS());
                            receiver.startMeasure(file,false);
                            }
                        }
                    });
                }
            });
        menuList.add(new MenuItemAction("Настройки") {
            @Override
            public void onSelect() {
                new SettingsMenu(MainActivity.this);
                }
            });
        menuList.add(new MenuItemAction("Выключить все") {
            @Override
            public void onSelect() {
                btViewFace.offAll();
                }
            });
        menuList.add(new MenuItemAction("Сброс BlueTooth") {
            @Override
            public void onSelect() {
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter.isEnabled())
                    mBluetoothAdapter.disable();
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBtIntent);
                }
            });
        menuList.add(new MenuItemAction("Список сенсоров") {
            @Override
            public void onSelect() {
                for(BTDescriptor descriptor : set.knownSensors)
                    addToLog("Датчик: "+descriptor.btName+": "+descriptor.btMAC);
                }
            });
        menuList.add(new MenuItemAction("Очистить список") {
            @Override
            public void onSelect() {
                set.knownSensors.clear();
                set.createMaps();
                saveSettings();
                }
            });
        menuList.add(new MenuItemAction("Отправить в mail") {
            @Override
            public void onSelect() {
                selectMultiFromArchive("Отправить Mail",sendMailSelector);
                }
            });
        menuList.add(new MenuItemAction("Просмотр волны") {
            @Override
            public void onSelect() {
                showWaveForm();
                }
            });
        menuList.add(new MenuItemAction("Конвертировать в wave") {
            @Override
            public void onSelect() {
                selectFromArchive("Конвертировать в wave",convertSelector);
                }
            });
        menuList.add(new MenuItemAction("Файл в архив") {
            @Override
            public void onSelect() {
                preloadFromText(CHOOSE_RESULT_COPY);
                }
            });
        menuList.add(new MenuItemAction("Файл кратко") {
            @Override
            public void onSelect() {
                fullInfo=false;
                hideFFTOutput=true;
                preloadFromText(CHOOSE_RESULT);
                }
            });
        menuList.add(new MenuItemAction("Файл подробно") {
            @Override
            public void onSelect() {
                fullInfo=true;
                hideFFTOutput=false;
                preloadFromText(CHOOSE_RESULT);
                }
            });
        menuList.add(new MenuItemAction("Образец") {
            @Override
            public void onSelect() {
                LEP500File file2 = new LEP500File(set,"Тест",gpsService.lastGPS());
                BTReceiver receiver = new BTReceiver(btViewFace,btViewFace.BTBack);
                receiver.startMeasure(file2,true);
                }
            });
        }
    //----------------------------------------------------------------------------------------------
    private  I_ArchveMultiSelector toGroupSelector = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(final FileDescriptionList fd, boolean longClick) {
            new SetOneParameter(MainActivity.this,"Группа","",true, new I_EventListener() {
                @Override
                public void onEvent(String subdir) {
                    File dd = new File(androidFileDirectory()+"/"+subdir);
                    if (dd.exists()){
                        popupAndLog(subdir+" уже существует");
                        return;
                        }
                    dd.mkdir();
                    for (FileDescription ff : fd){
                        try {
                            String src = androidFileDirectory()+"/"+ff.originalFileName;
                            moveFile(src, androidFileDirectory()+"/"+subdir+"/"+ff.originalFileName);
                            }catch (Exception ee){ addToLog(createFatalMessage(ee,5)); }
                        }
                    popupAndLog("Сгруппировано в "+subdir);
                }
            });
            }
        };
    private  I_ArchveMultiSelector fromGroupSelector = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fd, boolean longClick) {
            for (FileDescription ff : fd){
                ArrayList<FileDescription> dirList = createArchive(ff.originalFileName);
                for (FileDescription ff2 : dirList){
                    try {
                        moveFile(androidFileDirectory()+"/"+ff.originalFileName+"/"+ff2.originalFileName,
                            androidFileDirectory()+"/"+ff2.originalFileName);
                        }catch (Exception ee){ addToLog(createFatalMessage(ee,5)); }
                    }
                try {
                    File gg = new File(androidFileDirectory()+"/"+ff.originalFileName);
                    gg.delete();
                    }catch (Exception ee){ addToLog(createFatalMessage(ee,5)); }
                popupAndLog("Разгруппировано");
                }
            }
        };
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
            }
        };
    private  I_ArchveMultiSelector deleteMultiSelector = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fd, boolean longClick) {
            for (FileDescription ff : fd){
                File file = new File(androidFileDirectory()+"/"+ff.originalFileName);
                file.delete();
                }
            }
        };
    private  I_ArchveMultiSelector procViewMultiSelector = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fd, boolean longClick) {
            log.addView(createMultiGraph(R.layout.graphview));
            for (FileDescription ff : fd){
                procArchive(ff,false);
                }
            }
        };
    private  I_ArchveMultiSelector procViewSelectorFull = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fd, boolean longClick) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(),FullScreenGraph.class);
            AppData.ctx().setFileList(fd);
            startActivity(intent);
            }
        };
    private  I_ArchveMultiSelector procViewMultiSelectorFull = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fd, boolean longClick) {
            log.addView(createMultiGraph(R.layout.graphview));
            for (FileDescription ff : fd){
                procArchive(ff,true);
            }
        }
    };
    private  I_ArchveSelector convertSelector = new I_ArchveSelector() {
        @Override
        public void onSelect(FileDescription fd, boolean longClick) {
            String pathName = androidFileDirectory()+"/"+fd.originalFileName;
            FFTAudioTextFile xx = new FFTAudioTextFile();
            xx.setnPoints(set.nTrendPoints);
            hideFFTOutput=false;
            xx.convertToWave(pathName, new FFTAdapter(MainActivity.this,fd.toString()));
            }
        };
    private  I_ArchveMultiSelector sendMailSelector = new I_ArchveMultiSelector() {
        @Override
        public void onSelect(FileDescriptionList fdlist, boolean longClick) {
            try {
                final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{set.mailToSend});
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Звенящие опоры России");
                emailIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ArrayList<Uri> uris = new ArrayList<Uri>();
                for(FileDescription fd : fdlist){
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Датчик: " + fd.toString());
                    String filePath = androidFileDirectory() + "/" + fd.originalFileName;
                    addToLog(filePath);
                    File ff = new File(filePath);
                    Uri fileUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID, ff);
                    uris.add(fileUri);
                    //--------------- Старое -------------------------------------------------------
                    //emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,Uri.fromFile(ff));
                    //emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,Uri.parse(filePath));
                    }
                emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,uris);
                startActivity(Intent.createChooser(emailIntent, "Отправка письма..."));
                } catch (Exception ee){
                    addToLog("Ошибка mail: "+ee.toString());
                    }
            }
        };
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
    public void moveFile(String src, String dst) throws Exception {
        BufferedReader fd1 = new BufferedReader(new InputStreamReader(new FileInputStream(src),"Windows-1251"));
        BufferedWriter fd2 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst),"Windows-1251"));
        String ss;
        while ((ss=fd1.readLine())!=null){
            fd2.write(ss);
            fd2.newLine();
            }
        fd2.flush();
        fd1.close();
        fd2.close();
        File file = new File(src);
        file.delete();
        }
    //----------------------------------------------------------------------------------------------
    View.OnClickListener waveStartEvent = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new OneParameterDialog(MainActivity.this, "Параметр графика", "Начало (сек)", "" + waveStartTime, false, false,new I_EventListener() {
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
            new OneParameterDialog(MainActivity.this, "Параметр графика", "Масштаб", "" + waveMas, false, false,new I_EventListener() {
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

    public  void procWaveForm(FileDescription fd){
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
        final ArrayList<FileDescription> ss = createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : ss)
            out.add(ff.toString());
        new ListBoxDialog(this, out, title, new I_ListBoxListener() {
            @Override
            public void onSelect(int index) {
                selector.onSelect(ss.get(index),false);
                }
            @Override
            public void onLongSelect(int index) {
                selector.onSelect(ss.get(index),true);
                }
            }).create();
        }
    public void selectMultiFromArchive(String title, final I_ArchveMultiSelector selector){
        selectMultiFromArchive(false,title,selector);
        }
    public void selectMultiFromArchive(boolean dirList, String title, final I_ArchveMultiSelector selector){
        final ArrayList<FileDescription> ss = dirList ? createDirArchive() : createArchive();
        final ArrayList<String> list = new ArrayList<>();
        for(FileDescription ff : ss)
            list.add(dirList ? ff.originalFileName : ff.toString());
        new MultiListBoxDialog(this, title, list, new MultiListBoxListener() {
            @Override
            public void onSelect(boolean[] selected) {
                FileDescriptionList out = new FileDescriptionList() ;
                for(int i=0;i<ss.size();i++)
                    if (selected[i])
                        out.add(ss.get(i));
                    selector.onSelect(out,false);
                }
            });
    }
    public void showWaveForm(){
        final ArrayList<FileDescription> ss = createArchive();
        ArrayList<String> out = new ArrayList<>();
        for(FileDescription ff : ss)
            out.add(ff.toString());
        new ListBoxDialog(this, out, "Просмотр волны", new I_ListBoxListener() {
            @Override
            public void onSelect(int index) {
                procWaveForm(ss.get(index));
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
                    processInputStream(fis,ff.toString());
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
                    processInputStream(fis,ff.toString());
                    } catch (Throwable e) {
                        addToLog("Файл не открыт: "+ff.originalFileName+"\n"+e.toString());
                        return false;
                        }
                return true;
                }
            };
        addToLogButton(ff.toString(),listener,listenerLong);
        }
    public ArrayList<FileDescription>createArchive(){
        return createArchive(null);
        }
    public ArrayList<FileDescription> createArchive(String subdir){
        File ff = new File(androidFileDirectory()+(subdir!=null ? "/"+subdir : ""));
        if (!ff.exists()) {
            ff.mkdir();
            }
        ArrayList<FileDescription> out = new ArrayList<>();
        for(String ss : ff.list()){
            File file = new File(androidFileDirectory()+"/"+ss);
            if (file.isDirectory())
                continue;
            FileDescription dd = new FileDescription(ss);
            if (!dd.originalFileName.toUpperCase().endsWith(".TXT"))
                continue;
            String zz = dd.parseFromName();
            if (zz!=null)
                addToLog("Файл: "+ss+" "+zz);
            else
                out.add(dd);
            }
        out.sort(new Comparator<FileDescription>() {
            @Override
            public int compare(FileDescription o1, FileDescription o2) {
                return (int)(o2.createDate.getMillis() - o1.createDate.getMillis());
            }
        });
        return out;
        }
    public ArrayList<FileDescription> createDirArchive(){
        File ff = new File(androidFileDirectory());
        if (!ff.exists()) {
            ff.mkdir();
            }
        ArrayList<FileDescription> out = new ArrayList<>();
        for(String ss : ff.list()){
            File file = new File(androidFileDirectory()+"/"+ss);
            if (file.isDirectory())
                out.add(new FileDescription(ss));
            }
        return out;
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
        new OneParameterDialog(this, "Имя датчика",receiver.getSensorMAC(),"", false, true,new I_EventListener() {
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
