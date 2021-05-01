package me.romanow.lep500;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import romanow.snn_simulator.fft.FFT;

public class OneParameterDialog {
    private AlertDialog myDlg=null;
    private boolean wasChanged=false;
    private Activity base;

    private LinearLayout createItem(String name, String value,final EventListener lsn){
        return createItem(name,value,false,lsn);
        }
    private LinearLayout createItem(String name, String value, boolean shortSize,final EventListener lsn){
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
                }
            });
        img.setClickable(true);
        tt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId==6 || actionId==5){
                    lsn.onEvent(v.getText().toString());
                    }
                return false;
                }
        });
        return xx;
        }

    public OneParameterDialog(Activity base0, String title, String parName, String parValue, boolean shortSize, final  EventListener listener){
        base = base0;
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
            hd.setText(title);
            myDlg.setOnCancelListener(new DialogInterface.OnCancelListener(){
                public void onCancel(DialogInterface arg0) {
                    myDlg.cancel();
                    }
                });
            LinearLayout layout = createItem(parName, parValue, shortSize,new EventListener(){
                @Override
                public void onEvent(String ss) {
                    listener.onEvent(ss);
                    myDlg.cancel();
                    }});
            trmain.addView(layout);
            myDlg.setView(lrr);
            myDlg.show();
        } catch(Exception ee){
            int a=1;
            }
        catch(Error ee){
            int u=0;
        }
    }
}

