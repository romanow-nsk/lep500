package me.romanow.lep500;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class SetOneParameter {
    private MainActivity base;
    private AlertDialog myDlg=null;
    private boolean wasChanged=false;
    public SetOneParameter(MainActivity base0,String name, String value, boolean shortSize,final I_EventListener lsn){
        base = base0;
        myDlg = new AlertDialog.Builder(base).create();
        myDlg.setCancelable(true);
        myDlg.setTitle(null);
        RelativeLayout lrr = (RelativeLayout) base.getLayoutInflater().inflate(R.layout.settings, null);
        LinearLayout trmain = (LinearLayout) lrr.findViewById(R.id.dialog_settings_panel);
        trmain.setPadding(5, 5, 5, 5);
        TextView hd = (TextView) lrr.findViewById(R.id.dialog_settings_header);
        hd.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View arg0) {
                myDlg.cancel();
            }
        });
        myDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                myDlg.cancel();
            }
        });
        LinearLayout layout = createItem(name, value, shortSize, new I_EventListener(){
            @Override
            public void onEvent(String ss) {
                lsn.onEvent(ss);
                myDlg.cancel();
                }
            });
        trmain.addView(layout);
        myDlg.setView(lrr);
        myDlg.show();
        }
    public SetOneParameter(MainActivity base0,String name, final ArrayList<String> values, int idx, final I_ListBoxListener lsn){
        base = base0;
        myDlg = new AlertDialog.Builder(base).create();
        myDlg.setCancelable(true);
        myDlg.setTitle(null);
        RelativeLayout lrr = (RelativeLayout) base.getLayoutInflater().inflate(R.layout.settings, null);
        LinearLayout trmain = (LinearLayout) lrr.findViewById(R.id.dialog_settings_panel);
        trmain.setPadding(5, 5, 5, 5);
        TextView hd = (TextView) lrr.findViewById(R.id.dialog_settings_header);
        hd.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View arg0) {
                myDlg.cancel();
            }
        });
        myDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface arg0) {
                myDlg.cancel();
            }
        });
        LinearLayout layout = createListBox(name, values, idx, new I_ListBoxListener(){
            @Override
            public void onSelect(int index) {
                lsn.onSelect(index);
                myDlg.cancel();
                }
            @Override
            public void onLongSelect(int index) { }
            });
        trmain.addView(layout);
        myDlg.setView(lrr);
        myDlg.show();

        }
    private LinearLayout createItem(String name, String value, boolean shortSize,final I_EventListener lsn){
        LinearLayout xx=(LinearLayout)base.getLayoutInflater().inflate(
                shortSize ? R.layout.settings_item_short : R.layout.settings_item, null);
        xx.setPadding(5, 5, 5, 5);
        final EditText tt=(EditText) xx.findViewById(R.id.dialog_settings_value);
        tt.setText(""+value);
        TextView img=(TextView)xx.findViewById(R.id.dialog_settings_name);
        img.setText(name);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lsn.onEvent(tt.getText().toString());
                myDlg.cancel();
                }
            });
        img.setClickable(true);
        tt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==6 || actionId==5){
                    lsn.onEvent(v.getText().toString());
                    myDlg.cancel();
                    }
                return false;
                }
            });
        return xx;
        }
    private LinearLayout createListBox(String name, final ArrayList<String> values, int idx, final I_ListBoxListener lsn){
        LinearLayout xx=(LinearLayout)base.getLayoutInflater().inflate(R.layout.settings_item_list, null);
        xx.setPadding(5, 5, 5, 5);
        final TextView tt=(TextView) xx.findViewById(R.id.dialog_settings_value);
        tt.setText(""+values.get(idx));
        TextView img=(TextView)xx.findViewById(R.id.dialog_settings_name);
        img.setText(name);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ListBoxDialog(base,values,"Функ.окна", new I_ListBoxListener(){
                    @Override
                    public void onSelect(int index) {
                        lsn.onSelect(index);
                        tt.setText(""+values.get(index));
                        myDlg.cancel();
                        }
                    @Override
                    public void onLongSelect(int index) {}
                }).create();
            }
        });
        img.setClickable(true);
        return xx;
        }
}

