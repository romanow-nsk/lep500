/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;

// Сбор статистики по слою

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Comparator;

import me.romanow.lep500.FileDescription;
import me.romanow.lep500.I_FDComparator;


public class FFTStatistic {
    class SmoothArray{
        float data[];
        SmoothArray(int size){
            data = new float[size];
            for(int i=0;i<size;i++)
                data[i]=0;
            }
        void smoothOne(){
            int size = data.length;
            float out[] = new float[size];
            out[0]=(float)( 0.5*(data[1]+data[0]));
            for(int i=1;i<size-1;i++)
                out[i] = (float)( 0.5*(0.5*(data[i-1]+data[i+1])+data[i]));
            out[size-1]=(float)( 0.5*(data[size-2]+data[size-1]));
            data = out;
            }
        void smooth(int count){
            while (count-->0)
                smoothOne();
            }
        }
    private String name="";
    private int count=0;
    private int size=0;
    private boolean noReset=true;
    private float prev[]=null;
    private SmoothArray sumT=null;          // Сумма по времени
    private SmoothArray sum2T=null;         // Сумма квадратов по времени
    private SmoothArray sum2DiffF=null;     // Корреляция по частоте
    private SmoothArray sum2DiffT=null;     // Корреляция по времени
    public void reset() {
        noReset=true;
        }
    public void lasyReset(float data[]){
        if (!noReset)
            return;
        count=0;
        noReset=false;
        prev = data;
        size = data.length;
        sumT=new SmoothArray(size);
        sum2T=new SmoothArray(size);
        sum2DiffF=new SmoothArray(size);
        sum2DiffT=new SmoothArray(size);
        }
    public void smooth(int steps){
        sumT.smooth(steps);
        sum2T.smooth(steps);
        sum2DiffF.smooth(steps);
        sum2DiffT.smooth(steps);
        }
    public FFTStatistic(String name){
        setObjectName(name);
        reset();
        }
    public void addStatistic(float src[]) throws Exception{
        float data[] = src.clone();
        lasyReset(data);
        for(int i=0;i<size;i++){
            sumT.data[i]+=data[i];
            sum2T.data[i]+=data[i]*data[i];
            if (prev!=null)
                sum2DiffT.data[i]+=(data[i]-prev[i])*(data[i]-prev[i]);
            if (i!=0 && i!=size-1){
                sum2DiffF.data[i]+=(data[i]-data[i-1])*(data[i]-data[i-1]);
                sum2DiffF.data[i]+=(data[i]-data[i+1])*(data[i]-data[i+1]);
                }
            }
        prev = data;
        count++;
        }
    public int getCount(){
        return count;
        }
    //--------------- Среднее и дисперсия для массивов -------------------------
    private float getMid(float vv[]){
        float res=0;
        for(int i=0;i<size;i++)
            res+=vv[i];
        return res/size;
        }
    public float[] getDisps(float vv[]){
        float out[] = vv.clone();
        for(int i=0;i<size;i++){
            if (count==0)
                out[i]=0;
            else
                out[i] = (float)Math.sqrt(out[i]/count);
            }
        return out;
        }
    //--------------------------------------------------------------------------
    public float getDisp(){
        return getMid(getDisps(sum2T.data));
        }
    public float[] getMids(){
        float out[] = sumT.data.clone();
        for(int i=0;i<size;i++){
            if (count==0)
                out[i]=0;
            else
                out[i]/=count;
            }
        return out;
        }
    public float getMid(){
        return getMid(getMids());
        }
    public float[] getDisps(){
        return getDisps(sum2T.data);
        }
    public float[] getDiffsF(){
        return getDisps(sum2DiffF.data);
        }
    public float getDiffF(){
        return getMid(getDiffsF());
        }
    public float[] getDiffsT(){
        return getDisps(sum2DiffT.data);
        }
    public float getDiffT(){
        return getMid(getDiffsT());
        }

    private void sort(ArrayList<Extreme> list,Comparator<Extreme> comparator){
        int sz = list.size();
        for(int i=0;i<sz;i++)
            for(int j=1;j<sz;j++)
                if (comparator.compare(list.get(j-1),list.get(j))<0){
                    Extreme cc = list.get(j-1);
                    list.set(j-1,list.get(j));
                    list.set(j,cc);
                }
        }


    public ArrayList<Extreme> createExtrems(boolean byLevel, int nFirst, int nLast){
        return createExtrems(byLevel, nFirst, nLast,false);
        }
    public ArrayList<Extreme> createExtrems(boolean byLevel, int nFirst, int nLast, boolean ownSort){
        ArrayList<Extreme> out = new ArrayList<>();
        for(int i=nFirst+1;i<size-1-nLast;i++)
            if (sumT.data[i]>sumT.data[i-1] && sumT.data[i]>sumT.data[i+1]){
                int k1,k2;
                for(k1=i;k1>0 && sumT.data[k1]>sumT.data[k1-1];k1--);
                for(k2=i;k2<sumT.data.length-1 && sumT.data[k2]>sumT.data[k2+1];k2++);
                double d1 = sumT.data[i]-sumT.data[k1];
                double d2 = sumT.data[i]-sumT.data[k2];
                double diff = Math.sqrt((d1*d1+d2*d2)/2);
                out.add(new Extreme(sumT.data[i]/count,i,i* FFT.sizeHZ/2/size,diff/count));
                }
        if (!ownSort){
            if (byLevel)
                sort(out,new Comparator<Extreme>() {
                    @Override
                    public int compare(Extreme o1, Extreme o2) {
                        if (o1.value==o2.value) return 0;
                        return o1.value > o2.value ? -1 : 1;
                        }
                    });
            else
                sort(out,new Comparator<Extreme>() {
                    @Override
                    public int compare(Extreme o1, Extreme o2) {
                        if (o1.diff==o2.diff) return 0;
                        return o1.diff > o2.diff ? -1 : 1;
                    }
                });
            }
        else{
            Extreme xx[]=new Extreme[out.size()];
            out.toArray(xx);
            for(int i=1;i<xx.length;i++){
                for(int k=i; k>0 && (byLevel ? xx[k].value > xx[k-1].value : xx[k].diff > xx[k-1].diff);k--){
                    Extreme cc = xx[k];
                    xx[k] = xx[k-1];
                    xx[k-1]=cc;
                    }
                }
            out.clear();
            for(Extreme ex : xx)
                out.add(ex);
            }
        return out;
        }
    //--------------------------------------------------------------------------
    public String getTypeName() {
        return "Статистика";
        }
    public String getName() {
        return getObjectName();
        }    
    public String getObjectName() {
        return name;
        }
    public void setObjectName(String name) {
        this.name = name;
        }
    //------------------- Коррекция экспоненты--------------------------
    public double correctExp(int nPoints){
        float a0=sumT.data[0];
        double k=0;
        for(int i=0;i<nPoints;i++)
            k += -Math.log(sumT.data[i+1]/sumT.data[i]);
        k /=nPoints;
        for(int i=0;i<sumT.data.length;i++)
            sumT.data[i]-=a0*Math.exp(-k*i);
        return k;
        }
    //-----------------------------------------------------------------

    
}
