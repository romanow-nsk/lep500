package me.romanow.lep500;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import romanow.snn_simulator.layer.LayerStatistic;

public class FullScreenGraph extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.graph_gorizontal);
            LinearLayout lrr = (LinearLayout) findViewById(R.id.viewPanelHoriz);
            FileDescriptionList fd = AppData.ctx().getFileList();
            LinearLayout graph = createMultiGraph(R.layout.graphviewhoriz);
            lrr.addView(graph);
            for (FileDescription ff : fd) {
                procArchive(ff);
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
    public void showStatisticFull(LayerStatistic inputStat) {}
}
