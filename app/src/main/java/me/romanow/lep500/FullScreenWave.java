package me.romanow.lep500;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import me.romanow.lep500.fft.FFTAudioTextFile;
import me.romanow.lep500.fft.FFTStatistic;


public class FullScreenWave extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.graph_gorizontal);
            getSupportActionBar().hide();
            LinearLayout lrr = (LinearLayout) findViewById(R.id.viewPanelHoriz);
            LinearLayout hd = (LinearLayout) findViewById(R.id.viewPanelHead);
            FileDescriptionList fd = AppData.ctx().getFileList();
            LinearLayout graph = createMultiGraph(R.layout.graphviewhoriz,0);
            lrr.addView(graph);
            int idx=0;
            for (FileDescription ff : fd) {
                Button bb = new Button(this);
                bb.setTextColor(getPaintColor(idx) | 0xFF000000);
                bb.setBackgroundColor(0xFF00574B);
                bb.setTextSize(20);
                bb.setHeight(40);
                bb.setWidth(150);
                bb.setPadding(10,0,0,0);
                bb.setText(ff.srcNumber);
                hd.addView(bb);
                String fname = ff.originalFileName;
                try {
                    FileInputStream fis = new FileInputStream(androidFileDirectory()+"/"+fname);
                    addToLog(fd.toString(),greatTextSize);
                    FFTAudioTextFile xx = new FFTAudioTextFile();
                    xx.readData(ff,new BufferedReader(new InputStreamReader(fis, "Windows-1251")));
                    paintOne(xx.getData(),getPaintColor(idx++) | 0xFF000000);
                    } catch (Throwable e) {
                        addToLog("Файл не открыт: "+fname+"\n"+createFatalMessage(e,10));
                        }
                }
            } catch (Exception ee){
                addToLog(createFatalMessage(ee,10));
                }
        }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        }

    @Override
    public void clearLog() {}

    @Override
    public void addToLog(String ss, int textSize) {}

    @Override
    public void addToLogHide(String ss) {}

    @Override
    public void popupAndLog(String ss) {}

    @Override
    public void showStatisticFull(FFTStatistic inputStat, int idx) {}

}
