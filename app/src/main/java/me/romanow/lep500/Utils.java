package me.romanow.lep500;

public class Utils {
    public static double []calcTrend(double data[], int nPoints){
        double middles[] = new double[data.length];
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
    private static double expValues[]=null;         // Подчитаниие заранее значения экспоненты
    private static double dExp=0.01F;               // Шаг экспоненты
    private static double expLimit=50;              // Диапазон экспоненты
    public static void calcExp(){
        if (expValues!=null)
            return;
        expValues = new double[(int)(expLimit/dExp)];
        for(int i=0;i<expValues.length;i++)
            expValues[i] = (double)(Math.exp(-i*dExp));
        }
    public static double getExp(double x){
        if (x<0 || x>expLimit)
            return (double)(Math.exp(-x));
        calcExp();
        return expValues[(int)(x/dExp)];
        }
    public static double []convert(double in[]){
        double out[]=new double[in.length];
        for(int i=0;i<in.length;i++)
            out[i]=in[i];
        return out;
        }
    public static double[] reduceTo(double in[]){
        double out[] = new double[in.length/2];
        for(int i=0;i<out.length;i++)
            out[i]=in[i];
        return out;
        }
}
