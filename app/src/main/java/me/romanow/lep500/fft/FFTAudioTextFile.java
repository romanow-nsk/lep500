/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;


/**
 *
 * @author romanow
 */
public class FFTAudioTextFile implements FFTFileSource{
    protected String fspec=null;
    protected int sz=0;
    protected double data[]=null;
    protected int nPoints=0;             // Количество точек для удаления тренда
    //--------------------------------------------------------------------------------------
    private transient int cnum;
    private transient BufferedReader AudioFile=null;
    //---------------------------------------------------------------------------------------
    public void setnPoints(int nPoints) {
        this.nPoints = nPoints; }
    public void write_little_endian(int word, int num_bytes, OutputStream wav_file) throws IOException{
        int buf;
        while(num_bytes>0){   
            buf = word & 0xff;
            wav_file.write(buf);
    		num_bytes--;
            word >>= 8;
            }
        }       
    public void write_string(String ss, OutputStream wav_file) throws IOException{
        char cc[] = ss.toCharArray();
        for(int i=0;i<cc.length;i++){   
            wav_file.write((byte)cc[i]);
            }
        }
    public double[]  getData(){
        return data.clone();
        }
    public void readData(BufferedReader AudioFile) throws IOException {
        String in;
        for(int i=0;i<10;i++)           // 10 первых строк пропустить
            in = AudioFile.readLine();
        sz = Integer.parseInt(AudioFile.readLine());
        data = new double[sz];
        double mid=0,min=data[0],max=data[0];
        for(int i=0;i<sz;i++){
            data[i]=Integer.parseInt(AudioFile.readLine());
            mid += data[i];
            }
        int midd = (int)(mid/sz);
        for(int i=0;i<sz;i++){         // Убрать постоянную составляющую
            data[i] -= midd;
        }
        for(int i=0;i<sz;i++){
            if (data[i]>max) max=data[i];
            if (data[i]<min) min=data[i];
            }
        if (Math.abs(max)>Math.abs(min))
            min = max;
        min = Math.abs(min);
        min = Short.MAX_VALUE*0.9f/min;
        for(int i=0;i<sz;i++){
            data[i] *= min;
            }
        }
    public boolean convertToWave(String PatnToFile,FFTCallBack back){
        fspec=null;
        try {
            AudioFile = new BufferedReader(new FileReader(PatnToFile));
            } catch (FileNotFoundException ex) {
                return false;
                }
        String in;
        try {
            readData(AudioFile);
            removeTrend(nPoints);
            int k = PatnToFile.lastIndexOf(".");
            String outname = PatnToFile.substring(0, k)+".wav";
            FileOutputStream wav_file = new FileOutputStream(outname);
        	int sample_rate;
            int num_channels;
            int bytes_per_sample;
            int byte_rate;
            int num_samples = data.length;
            int i;  
            num_channels = 1;  
            bytes_per_sample = 2;
            sample_rate = 44100; //100; // 44100;
        	byte_rate = sample_rate*num_channels*bytes_per_sample;
        	write_string("RIFF", wav_file);
            write_little_endian(36 + bytes_per_sample* num_samples*num_channels, 4, wav_file);
            write_string("WAVE", wav_file);
            write_string("fmt ", wav_file);
            write_little_endian(16, 4, wav_file);   
            write_little_endian(1, 2, wav_file);    
            write_little_endian(num_channels, 2, wav_file);
            write_little_endian(sample_rate, 4, wav_file);
            write_little_endian(byte_rate, 4, wav_file);
            write_little_endian(num_channels*bytes_per_sample, 2, wav_file);  
            write_little_endian(8*bytes_per_sample, 2, wav_file);  
            write_string("data", wav_file);
            write_little_endian(bytes_per_sample* num_samples*num_channels, 4, wav_file);
            for (i=0; i< num_samples; i++){
                write_little_endian((short)data[i],bytes_per_sample, wav_file);
                }   
            wav_file.flush();
            wav_file.close();
            back.onMessage("Записано "+num_samples+" сэмплов, "+ ((double)num_samples)/sample_rate+ " сек");
            fspec = PatnToFile;
            close();
            return true;
            } catch(Exception ee){
                back.onError(ee);
                close();
                return false;
                }
        }        
    public final static int Test=0;
    public final static int Open=1;
    public final static int OpenAndPlay=2;
    public boolean testAndOpenFile(int mode, String PatnToFile, int sizeHZ, FFTCallBack back){
        try {
            AudioFile = new BufferedReader(new FileReader(PatnToFile));
            } catch (FileNotFoundException ex) {
                return false;
                }
        if (mode==Test){
            close();
            return true;
            }
        String in;
        try {
            readData(AudioFile);
            removeTrend(nPoints);
            close();
            cnum=0;
            return true;
            } catch(Exception ee){
                close();
                return false;
                }
        }

    @Override
    public String getFileSpec() {
        return fspec;
        }

    @Override
    public String testSource(int sizeHZ) {
        // Формат не проверяется
        return null;
        }

    @Override
    public long getFrameLength() {
        return sz;
        }
    @Override
    public int read(double[] buf, int offset, int lnt) throws IOException {
        if (sz==0)
            return 0;
        int cnt=0;
        for(int i=offset; i<offset+lnt && cnum<sz; i++){
            buf[i] = data[cnum] /Short.MAX_VALUE;
            //System.out.println("text "+i+" "+buf[i]);
            cnum++;
            cnt++;
            }
        return cnt;
        }
    @Override
    public void close() {
        try {
            if (AudioFile!=null){
                AudioFile.close();
                }
            } catch (IOException ex) {}
        }

    public String getTypeName() {
        return "Текстовый файл";
        }
    public String getName() {
        return getTypeName();
        }

    @Override
    public double getSampleRate() {
        return 44100;
        }
    public void removeTrend(int nPoints){
        if (nPoints==0)
            return;
        double middles[] = new double[data.length];
        for(int i=0;i<data.length;i++){
            middles[i]=0;
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
        for(int ii=0;ii<data.length;ii++)
            data[ii]-=middles[ii];
        }
}
