package me.romanow.lep500;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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
import romanow.snn_simulator.fft.FFTAudioSource;
import romanow.snn_simulator.fft.FFTAudioTextFile;
import romanow.snn_simulator.fft.FFTCallBack;
import romanow.snn_simulator.fft.FFTParams;
import romanow.snn_simulator.layer.Extreme;
import romanow.snn_simulator.layer.LayerStatistic;

public class MainActivity extends AppCompatActivity {
    private FFT fft = new FFT();
    private LayerStatistic inputStat = new LayerStatistic("Входные данные");
    private final int  p_BlockSize=2;
    private final int  p_OverProc=95;
    private final boolean  p_LogFreq=false;
    private final boolean  p_Compress=true;
    private final int  compressLevel=25;
    private final int  p_SubToneCount=1;
    private boolean isLoaded=false;
    private int nFirst=5;
    private int nSmooth=50;
    //----------------------------------------------------------------------------
    private LinearLayout log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            log = (LinearLayout) findViewById(R.id.log);
            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
        } catch (Exception ee){ addToLog(ee.toString());}
    }

    private void addToLog(String ss){
        TextView txt = new TextView(this);
        txt.setText(ss);
        log.addView(txt);
        }


    private final int CHOOSE_RESULT=10;
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
                xx.readData(new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri), "Windows-1251")));
                fft.setFFTParams(new FFTParams(p_BlockSize*FFT.Size0,p_OverProc, p_LogFreq,p_SubToneCount, false, false,false,0,1.0));
                addToLog("Отсчетов "+xx.getFrameLength());
                isLoaded=true;
                fft.setLogFreqMode(p_LogFreq);
                fft.setCompressMode(p_Compress);
                fft.setCompressGrade(compressLevel);
                inputStat.reset();
                fft.fftDirect(xx,back);
                } catch (Exception ee){
                    addToLog(ee.toString());
                    }
            }
        }

    private String showStatistic(){
        String out = "Отсчетов:"+inputStat.getCount()+"\n";
        double mid =inputStat.getMid();
        out+="Среднее:"+mid+"\n";
        out+="Приведенное станд.откл:"+inputStat.getDisp()/mid+"\n";
        out+="Приведенная неравн.по T:"+inputStat.getDiffT()/mid+"\n";
        out+="Приведенная неравн.по F:"+inputStat.getDiffF()/mid+"\n";
        ArrayList<Extreme> list = inputStat.createExtrems();
        int count = nFirst < list.size() ? nFirst : list.size();
        Extreme extreme = list.get(0);
        double val0 = extreme.value;
        out+=String.format("Макс=%6.4f f=%d гц",extreme.value,(int)extreme.freq)+"\n";
        double sum=0;
        for(int i=1; i<count;i++){
            extreme = list.get(i);
            double proc = extreme.value*100/val0;
            sum+=proc;
            out+=String.format("Макс=%6.4f f=%d гц %d%% к первому",extreme.value,(int)extreme.freq,(int)proc)+"\n";
        }
        out+=String.format("Средний - %d%% к первому",(int)(sum/(count-1)))+"\n";
        return out;
        }
    //--------------------------------------------------------------------------
    private FFTCallBack back = new FFTCallBack(){
        @Override
        public void onStart(float msOnStep) {
            }
        @Override
        public void onFinish() {
            inputStat.smooth(nSmooth);
            addToLog(showStatistic());
            }
        @Override
        public boolean onStep(int nBlock, int calcMS, float totalMS, FFT fft) {
                long tt = System.currentTimeMillis();
                float lineSpectrum[] = fft.getSpectrum();
                boolean xx;
                try {
                    inputStat.addStatistic(lineSpectrum);
                    } catch (Exception ex) {
                        addToLog(ex.toString());
                        return false;
                        }
                return true;
            }
        @Override
        public void onError(Exception ee) {
            addToLog("1."+ee.toString());
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
        return super.onOptionsItemSelected(item);
    }
    //--------------------------------------------------------------------------------------------------------
    public void popupToast(int viewId, String ss) {
        Toast toast3 = Toast.makeText(getApplicationContext(), ss, Toast.LENGTH_LONG);
        //LinearLayout lrr = (LinearLayout) getLayoutInflater().inflate(R.layout.toast,null);
        //((TextView)lrr.findViewById(R.id.toastText)).setText(ss);
        //toast3.setGravity(Gravity.BOTTOM, 0, 100);
        //toast3.setView(lrr);
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
}
