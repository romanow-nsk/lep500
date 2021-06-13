/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;

//     Нормальзованный массив данных

import me.romanow.lep500.Utils;

public class FFTArray {
    protected double data[]=new double[0];
    private double diff[]=new double[0];
    private double max=0;
    private int count=0;
    private double delta=0.95F;
    public int size(){
        return data.length;
        }
    public void clear(){
        count=0;
        max=0;
        for (int i=0;i<data.length;i++){
            data[i]=0;
            diff[i]=0;
            }
        }
    public double getSumDiff(int i){
        if (count==0)
            return 0;
        return (double)(Math.sqrt(diff[i])/count);
        }
    public int getCount(){
        return count;
        }
    public void nextStep(){
        max *= delta;
        count++;
        }
    public FFTArray(int size){
        data = new double[size];
        diff = new double[size];
        clear();
        }
    public void clearMax(){
        max=0;
        }
    public double getMax(){
        return max;
        }
    public double get(int i){
        if (i<0 || i>=data.length)
            return 0;
        return data[i];
        }
    public void set(int i, double value){
        if (i<0 || i>=data.length)
            return;
        diff[i] += Math.abs(data[i]-value);
        count++;
        data[i] = value;
        if (value > max)
            max = value;
        }

    public void calcMax(){
        double max = data[0];
        count = data.length;
        for(int i=0;i<data.length;i++){
            diff[i]=0;
            if (data[i] > max)
                max = data[i];
            }
        }
    public void normalize(double k){
        for (int i=0;i<data.length;i++)
            data[i] *= k;
        }    
    public void normalize(){
        for (int i=0;i<data.length;i++)
            data[i]/= max;
        }    
    public double []getNormalized(double k){
        double out[] = (double[])data.clone();
        for (int i=0;i<data.length;i++)
            out[i] *= k;
        return out;
        }
    public double []getNormalized(){
        double out[] = (double[])data.clone();
        if (max==0)
            return out;
        for (int i=0;i<data.length;i++)
            out[i] /=max;
        return out;
        }
    public void compress(boolean compressMode, double compressGrade,double k){
        normalize(k);
        if (!compressMode)
            return;
        for(int i=0;i<data.length;i++){
            data[i] = 1- Utils.getExp(compressGrade*data[i]/max);
            }
        }
    public double []getCompressed(boolean compressMode, double compressGrade, double k){
        if (!compressMode)
            return getNormalized();
        double out[] = getNormalized(k);
        for(int i=0;i<out.length;i++){
            out[i] = 1-Utils.getExp(compressGrade*out[i]/max);
            }
        return out;
        }
    public double []getOriginal(){
        return data.clone();
        }
}
