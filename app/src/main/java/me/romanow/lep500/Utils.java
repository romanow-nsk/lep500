package me.romanow.lep500;

public class Utils {
    public static float []calcTrend(float data[], int nPoints){
        float middles[] = new float[data.length];
        for(int i=0;i<data.length;i++){
            middles[i]=0;
        }
        if (nPoints==0) return middles;
        for(int i=0;i<data.length;i++){
            for(int j=i-nPoints;j<=i+nPoints;j++){
                if (j<0)
                    middles[i]+=data[0];
                else
                if(j>=data.length)
                    middles[i]+=data[data.length-1];
                else
                    middles[i]+=data[j];
            }
            middles[i]/=2*nPoints+1;
        }
        return middles;
    }
    //------------- СТАТИЧЕСКАЯ ЧАСТЬ
    private static float expValues[]=null;         // Подчитаниие заранее значения экспоненты
    private static float dExp=0.01F;               // Шаг экспоненты
    private static float expLimit=50;              // Диапазон экспоненты
    public static void calcExp(){
        if (expValues!=null)
            return;
        expValues = new float[(int)(expLimit/dExp)];
        for(int i=0;i<expValues.length;i++)
            expValues[i] = (float)(Math.exp(-i*dExp));
        }
    public static float getExp(float x){
        if (x<0 || x>expLimit)
            return (float)(Math.exp(-x));
        calcExp();
        return expValues[(int)(x/dExp)];
        }
    public static double []convert(float in[]){
        double out[]=new double[in.length];
        for(int i=0;i<in.length;i++)
            out[i]=in[i];
        return out;
        }
    public static float []convert(double in[]){
        float out[]=new float[in.length];
        for(int i=0;i<in.length;i++)
            out[i]=(float)in[i];
        return out;
        }
    public static float[] reduceTo(float in[]){
        float out[] = new float[in.length/2];
        for(int i=0;i<out.length;i++)
            out[i]=in[i];
        return out;
        }
}
