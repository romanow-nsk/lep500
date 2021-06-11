package me.romanow.lep500;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import me.romanow.lep500.fft.FFTStatistic;


public class FullScreenGraph extends BaseActivity {
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
            defferedStart();
            for (FileDescription ff : fd) {
                Button bb = new Button(this);
                bb.setTextColor(paintColors[colorNum] | 0xFF000000);
                bb.setBackgroundColor(0xFF00574B);
                bb.setTextSize(20);
                bb.setHeight(40);
                bb.setWidth(150);
                bb.setPadding(10,0,0,0);
                bb.setText(ff.srcNumber);
                hd.addView(bb);
                procArchive(ff);
                }
            defferedFinish();
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
    public void showStatisticFull(FFTStatistic inputStat) {
        }

}
