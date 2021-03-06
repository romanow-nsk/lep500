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
import me.romanow.lep500.Utils;


public class FFTStatistic {
    class SmoothArray extends FFTArray{
        public SmoothArray(int size){
            super(size);
            }
        void smoothOne(){
            int size = data.length;
            double out[] = new double[size];
            out[0]=(double)( 0.5*(data[1]+data[0]));
            for(int i=1;i<size-1;i++)
                out[i] = (double)( 0.5*(0.5*(data[i-1]+data[i+1])+data[i]));
            out[size-1]=(double)( 0.5*(data[size-2]+data[size-1]));
            data = out;
            }
        void smooth(int count){
            while (count-->0)
                smoothOne();
            }
        }
    public final static int SortAbs=0;
    public final static int SortDiff=1;
    public final static int SortTrend=2;
    private String name="";
    private int count=0;
    private int size=0;
    private boolean noReset=true;
    private double prev[]=null;
    private SmoothArray sumT=null;          // Сумма по времени
    private SmoothArray sum2T=null;         // Сумма квадратов по времени
    private SmoothArray sum2DiffF=null;     // Корреляция по частоте
    private SmoothArray sum2DiffT=null;     // Корреляция по времени
    private SmoothArray normalized = null;
    public void reset() {
        noReset=true;
        }
    public void lasyReset(double data[]){
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
    public void addStatistic(double src[]) throws Exception{
        double data[] = src.clone();
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
    private double getMid(double vv[]){
        double res=0;
        for(int i=0;i<size;i++)
            res+=vv[i];
        return res/size;
        }
    public double[] getDisps(double vv[]){
        double out[] = vv.clone();
        for(int i=0;i<size;i++){
            if (count==0)
                out[i]=0;
            else
                out[i] = (double)Math.sqrt(out[i]/count);
            }
        return out;
        }
    //--------------------------------------------------------------------------
    public double getDisp(){
        return getMid(getDisps(sum2T.data));
        }
    public double[] getMids(){
        double out[] = sumT.data.clone();
        for(int i=0;i<size;i++){
            if (count==0)
                out[i]=0;
            else
                out[i]/=count;
            }
        return out;
        }
    public double normalizeStart(){
        if (count==0) return 0;
        normalized = new SmoothArray(size);
        for(int i=0;i<normalized.data.length;i++)
            normalized.data[i]=sumT.data[i]/count;
        double max=normalized.data[0];
        for(double vv : normalized.data)
            if (vv > max)
                max = vv;
        return max;
        }
    public void normalizelFinish(double max){
        for(int i=0;i<normalized.data.length;i++)
            normalized.data[i]/=max;
        }
    public double normalizeMax(){
        if (count==0) return 0;
        double max=sumT.data[0];
        for(double vv : sumT.data)
            if (vv > max)
                max = vv;
        return (double) max/count;
        }
    public double getMid(){
        return getMid(getMids());
        }
    public double[] getDisps(){
        return getDisps(sum2T.data);
        }
    public double[] getDiffsF(){
        return getDisps(sum2DiffF.data);
        }
    public double getDiffF(){
        return getMid(getDiffsF());
        }
    public double[] getDiffsT(){
        return getDisps(sum2DiffT.data);
        }
    public double getDiffT(){
        return getMid(getDiffsT());
        }
    public double[] getNormalized() { return normalized.data; }

    private void sort(ArrayList<Extreme> list, Comparator<Extreme> comparator){
        int sz = list.size();
        for(int i=1;i<sz;i++)
            for(int j=i;j>0 && comparator.compare(list.get(j-1),list.get(j))>0;j--){
                Extreme cc = list.get(j-1);
                list.set(j-1,list.get(j));
                list.set(j,cc);
                }
        }
    //---------------------------- Компараторы для видов сортировки
    Comparator<Extreme> comparatorList[]= new Comparator[]{
        new Comparator<Extreme>() {
            @Override
            public int compare(Extreme o1, Extreme o2) {
                if (o1.value==o2.value) return 0;
                return o1.value > o2.value ? -1 : 1;
                }
            },
        new Comparator<Extreme>() {
            @Override
            public int compare(Extreme o1, Extreme o2) {
                if (o1.diff==o2.diff) return 0;
                return o1.diff > o2.diff ? -1 : 1;
                }
            },
        new Comparator<Extreme>() {
            @Override
            public int compare(Extreme o1, Extreme o2) {
                double vv1 = o1.trend;
                double vv2 = o2.trend;
                if (vv1==vv2) return 0;
                return vv1 > vv2 ? -1 : 1;
            }
        },
    };

    public ArrayList<Extreme> createExtrems(int mode, int nFirst, int nLast, int trendPointsNum){
        return createExtrems(mode, nFirst, nLast,false,trendPointsNum,1);
        }
    public ArrayList<Extreme> createExtrems(int mode, int nFirst, int nLast, boolean ownSort, int trendPointsNum,int nMid){
        ArrayList<Extreme> out = new ArrayList<>();
        double data[] = normalized.getOriginal();
        double trend[] = Utils.calcTrend(data,trendPointsNum);
        for(int i=nFirst+1;i<size-1-nLast;i++)
            if (data[i]>data[i-1] && data[i]>data[i+1]){
                int k1,k2,k3,k4;
                for(k1=i;k1>0 && data[k1]>data[k1-1];k1--);
                for(k2=i;k2<data.length-1 && data[k2]>data[k2+1];k2++);
                for(k3=i;k3>=0 && data[k3]>data[i]/2;k3--);
                for(k4=i;k4<data.length && data[k4]>data[i]/2;k4++);
                double d1 = data[i]-data[k1];
                double d2 = data[i]-data[k2];
                double diff = Math.sqrt((d1*d1+d2*d2)/2);
                int decrem=k4-k3;
                if (k3<k1 || k4>k2)
                    decrem = -1;
                double dd = 0;
                for(int j=i-nMid;j<=i+nMid;j++){
                    if (j<0 || j>=data.length)
                        dd+=data[i];
                    else
                        dd+=data[j];
                    }
                out.add(new Extreme(dd/(2*nMid+1),i,diff,(data[i]-trend[i]),decrem));
                }
        sort(out,comparatorList[mode]);
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
        double a0=sumT.data[0];
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
