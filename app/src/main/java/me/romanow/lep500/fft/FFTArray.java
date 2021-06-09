/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;

//     Нормальзованный массив данных

public class FFTArray {
    private float data[]=new float[0];
    private float diff[]=new float[0];
    private float max=0;
    private int count=0;
    private float delta=0.95F;
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
    public float getSumDiff(int i){
        if (count==0)
            return 0;
        return (float)(Math.sqrt(diff[i])/count);
        }
    public int getCount(){
        return count;
        }
    public void nextStep(){
        max *= delta;
        count++;
        }
    public FFTArray(int size){
        data = new float[size];
        diff = new float[size];
        clear();
        }
    public void clearMax(){
        max=0;
        }
    public float getMax(){
        return max;
        }
    public float get(int i){
        if (i<0 || i>=data.length)
            return 0;
        return data[i];
        }
    public void set(int i, float value){
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
    public void normalize(float k){
        for (int i=0;i<data.length;i++)
            data[i] *= k;
        }    
    public void normalize(){
        for (int i=0;i<data.length;i++)
            data[i]/= max;
        }    
    public float []getNormalized(float k){
        float out[] = (float[])data.clone();
        for (int i=0;i<data.length;i++)
            out[i] *= k;
        return out;
        }
    public float []getNormalized(){
        float out[] = (float[])data.clone();
        if (max==0)
            return out;
        for (int i=0;i<data.length;i++)
            out[i] /=max;
        return out;
        }
    public void compress(boolean compressMode, float compressGrade,float k){
        normalize(k);
        if (!compressMode)
            return;
        for(int i=0;i<data.length;i++){
            data[i] = 1-FFT.getExp(compressGrade*data[i]/max);
            }
        }
    public float []getCompressed(boolean compressMode, float compressGrade, float k){
        if (!compressMode)
            return getNormalized();
        float out[] = getNormalized(k);
        for(int i=0;i<out.length;i++){
            out[i] = 1-FFT.getExp(compressGrade*out[i]/max);
            }
        return out;
        }
    public float []getOriginal(){
        return (float[])data.clone();
        }
   //---------------- СТАТИЧЕСКАЯ ЧАСТЬ ---------------------------------------
   public static void normalizeLocal(float in[]){
        float max=in[0];
        for(int i=0;i<in.length;i++){
            if (in[i]>max)
                max = in[i];
            }
        if (max==0)
            return;
        for(int i=0;i<in.length;i++){
            in[i]/=max;
            }        
        }
}
