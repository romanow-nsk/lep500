package me.romanow.lep500;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;

import romanow.snn_simulator.fft.FFT;

public class MenuSettings{
    private AlertDialog myDlg=null;
    private boolean wasChanged=false;
    public MenuSettings(MainActivity base0){
        base = base0;
        dialogMain();
        }
    private LinearLayout createItem(String name, String value, final ValueListener lsn){
        LinearLayout xx=(LinearLayout)base.getLayoutInflater().inflate(R.layout.settings_item, null);
        xx.setPadding(5, 5, 5, 5);
        final EditText tt=(EditText) xx.findViewById(R.id.dialog_settings_value);
        tt.setText(""+value);
        TextView img=(TextView)xx.findViewById(R.id.dialog_settings_name);
        img.setText(name);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lsn.onValue(true,tt.getText().toString());
                }
            });
        img.setClickable(true);
        tt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==6 || actionId==5){
                    lsn.onValue(true, v.getText().toString());
                    }
                return false;
            }
        });
        return xx;
        }
    private LinearLayout createListBox(String name, final ArrayList<String> values, int idx, final ListBoxListener lsn){
        LinearLayout xx=(LinearLayout)base.getLayoutInflater().inflate(R.layout.settings_item_list, null);
        xx.setPadding(5, 5, 5, 5);
        final TextView tt=(TextView) xx.findViewById(R.id.dialog_settings_value);
        tt.setText(""+values.get(idx));
        TextView img=(TextView)xx.findViewById(R.id.dialog_settings_name);
        img.setText(name);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ListBoxDialog(base,values,"Функ.окна", new ListBoxListener(){
                    @Override
                    public void onSelect(int index) {
                        lsn.onSelect(index);
                        tt.setText(""+values.get(index));
                        }
                    @Override
                    public void onLongSelect(int index) {}
                }).create();
            }
        });
        img.setClickable(true);
        return xx;
    }
    private MainActivity base;
    private void settingsChanged(){
        base.saveSettings();
        wasChanged=true;
        base.popupInfo("Настройки изменены");
        }
    public void dialogMain(){
        try {
            myDlg=new AlertDialog.Builder(base).create();
            myDlg.setCancelable(true);
            myDlg.setTitle(null);
            RelativeLayout lrr=(RelativeLayout)base.getLayoutInflater().inflate(R.layout.settings, null);
            LinearLayout trmain=(LinearLayout)lrr.findViewById(R.id.dialog_settings_panel);
            trmain.setPadding(5, 5, 5, 5);
            TextView hd=(TextView)lrr.findViewById(R.id.dialog_settings_header);
            hd.setOnClickListener(new View.OnClickListener(){
                public void onClick(final View arg0) {
                    myDlg.cancel();
                }});
            myDlg.setOnCancelListener(new DialogInterface.OnCancelListener(){
                public void onCancel(DialogInterface arg0) {
                    myDlg.cancel();
                    }
                });
            LinearLayout layout = createItem("Частота мин.", String.format("%4.2f",base.set.FirstFreq), new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        ss = ss.trim().replace(",",".");
                        base.set.FirstFreq=Double.parseDouble(ss);
                        settingsChanged();
                        } catch (Exception ee){
                           base.popupInfo("Формат числа");}
                            }
                    });
            trmain.addView(layout);
            layout = createItem("Частота макс.", String.format("%4.2f",base.set.LastFreq), new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        ss = ss.trim().replace(",",".");
                        base.set.LastFreq=Double.parseDouble(ss);
                        settingsChanged();
                    } catch (Exception ee){
                        base.popupInfo("Формат числа");}
                        }
                });
            trmain.addView(layout);
            layout = createItem("Блоков*1024", ""+base.set.p_BlockSize, new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        base.set.p_BlockSize=Integer.parseInt(ss);
                        settingsChanged();
                        } catch (Exception ee){
                            base.popupInfo("Формат числа");}
                            }
                });
            trmain.addView(layout);
            layout = createItem("% перекрытия", ""+base.set.p_OverProc, new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        base.set.p_OverProc=Integer.parseInt(ss);
                        settingsChanged();
                    } catch (Exception ee){
                        base.popupInfo("Формат числа");}
                }
            });
            trmain.addView(layout);
            layout = createItem("Сглаживание", ""+base.set.kSmooth, new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        base.set.kSmooth=Integer.parseInt(ss);
                        settingsChanged();
                    } catch (Exception ee){
                        base.popupInfo("Формат числа");}
                }
            });
            trmain.addView(layout);
            layout = createItem("ФВЧ (точек)", ""+base.set.nTrendPoints, new ValueListener(){
                @Override
                public void onValue(boolean bb,String ss) {
                    try {
                        base.set.nTrendPoints=Integer.parseInt(ss);
                        settingsChanged();
                    } catch (Exception ee){
                        base.popupInfo("Формат числа");}
                }
            });
            trmain.addView(layout);
            layout = createListBox("Функ.окна", FFT.winFuncList, base.set.winFun, new ListBoxListener() {
                @Override
                public void onSelect(int index) {
                    base.set.winFun = index;
                    settingsChanged();
                    }
                @Override
                public void onLongSelect(int index) {}
                });
            trmain.addView(layout);
            myDlg.setView(lrr);
            myDlg.show();
        } catch(Exception ee){
            int a=1;
            }
        catch(Error ee){  }
    }
}

