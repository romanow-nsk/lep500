package me.romanow.lep500.yandexmap;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class Arrow{
    int img_sz=64;
    Drawable icon;
    ImageView IM;
    int x00,y00;
    int xx,yy;
    void refresh(){ refresh(x00,y00); }
    void refresh(int xx,int yy){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(img_sz,img_sz);
        params.setMargins(xx, yy, -1, -1);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        IM.setLayoutParams(params);
    }
    public int getX(){ return x00+img_sz;}
    public int getY(){ return y00+img_sz;}
    public ImageView getIM(){ return IM; }
    public Arrow(Activity activity,Drawable ic, int x0, int y0, View.OnClickListener click){
        icon=ic;
        x00=x0; y00=y0;
        IM=new ImageView(activity);
        IM.setClickable(true);
        if (click!=null){
            IM.setClickable(true);
            IM.setOnClickListener(click);
        }
        //----------- ВСЕ СОБЫТИЯ НА ТОМ ОБЪЕКТЕ, НА КОТОРОМ DOWN
        /*
        IM.setOnTouchListener(new OnTouchListener(){
            public boolean onTouch(View arg0, MotionEvent arg1) {
                int nn=arg1.getAction();
                if (nn==arg1.ACTION_DOWN){
                    xx=(int)arg1.getRawX();
                    yy=(int)arg1.getRawY();
                	}
                if (nn==arg1.ACTION_MOVE){
                    int x11=(int)arg1.getRawX();
                    int y11=(int)arg1.getRawY();
                    refresh(x00+(int)arg1.getRawX()-xx,y00+(int)arg1.getRawY()-yy);
                    }
                if (nn==arg1.ACTION_UP){
                	x00=x00+(int)arg1.getRawX()-xx;
                	y00=y00+(int)arg1.getRawY()-yy;
                    refresh();
                    }
                return false;
            }});
        */
        refresh();
        IM.setImageDrawable(icon);
    }
}

