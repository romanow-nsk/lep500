package me.romanow.lep500;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class LEP500File{
    private DateTime createDate = new DateTime();
    private String  sensorName;                  // Номер датчика
    private LEP500Settings settings;
    private GPSPoint gps;
    private short data[];
    public LEP500File(LEP500Settings settings0, String sensorName0, GPSPoint gps0){
        settings = settings0;
        sensorName = sensorName0;
        gps = gps0;
        }
    public LEP500Settings getSettings(){
        return settings;
        }
    public void setData(short data0[]){
        data = data0;
        }
    public short []getData(){  return data; }
    public String createOriginalFileName(){
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyyMMdd");
        DateTimeFormatter dtf2 = DateTimeFormat.forPattern("HHmmss");
        return dtf.print(createDate)+"T"+dtf2.print(createDate)+"_"+ settings.measureCounter+"-"+sensorName+"_"+settings.measureGroup+".txt";
        }
    public void createTestMeasure(){
        int v0=1000;
        int ampl = 200;
        // 2pi = 100гц = 10 мс, 2 гц = 500 мс, период = 250 отсчетов
        for(int i=0;i<data.length;i++){
            data[i]=(short) (v0/5+1000*Math.sin(i*2*Math.PI/25.)+500*Math.sin(i*2*Math.PI/10.));
            v0++;
            }
        }
    public void save(String path, I_EventListener back){
        FileOutputStream out=null;
        try {
            String fspec = path+"/"+createOriginalFileName();
            out = new FileOutputStream(fspec);
            BufferedWriter os = new BufferedWriter(new OutputStreamWriter(out,"Windows-1251"));
            //1 16 октября 2020г. 16:53:01
            DateTimeFormatter dtf = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss");
            os.write(dtf.print(createDate));
            os.newLine();
            //2 CM-316 ужур сора 1 цепь опора 352
            os.write(settings.measureGroup+" "+settings.measureTitle);
            os.newLine();
            //3 -------
            os.write(gps.toStrY());
            os.newLine();
            //4 -------
            os.write(gps.toStrX());
            os.newLine();
            //5 0
            os.write("0");
            os.newLine();
            //6 16 бит  тдм 003
            os.write("16 бит");
            os.newLine();
            //7 1
            os.write("1");
            os.newLine();
            //8
            os.write("");
            os.newLine();
            //9 10000
            os.write(""+(int)(settings.measureFreq*100));
            os.newLine();
            //10 канал-1 баланс=128 температура=8C
            os.write("канал-"+sensorName);
            os.newLine();
            os.write(""+data.length);
            os.newLine();
            for(int i=0;i<data.length;i++) {
                os.write(""+data[i]);
                os.newLine();
                }
            os.flush();
            os.close();
            out.close();
        } catch (Exception e) {
            back.onEvent("Ошибка записи в файл "+createOriginalFileName()+": "+e.toString());
            if (out!=null) {
                try {
                    out.close();
                    } catch (IOException ex) {}
                }
            }
    }
}
