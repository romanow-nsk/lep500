package me.romanow.lep500;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import me.romanow.lep500.fft.FFTAudioTextFile;

public class FileDescription extends FFTAudioTextFile {
    public DateTime createDate = new DateTime();
    public String lepNumber="";            // Номер опоры (группа)
    public String srcNumber="";            // Номер датчика
    public String comment="";              // Комментарий
    public String originalFileName="";     // Оригинальное имя
    public GPSPoint gps = new GPSPoint();
    public double fileFreq = 0;          // Частота измерений из файла
    public String fileGroupTitle="";     // Группа-опора из файла
    public String fileDateTime="";       // Дата-время создания из файла
    public String fileSensorName="";     // Имя сенсора из файла
    public int fileMeasureCounter=0;     // Последовательный номер измерения из файла
    public String parseFromName() {
        try{
            String ss = originalFileName.toLowerCase();
            if (!ss.endsWith(".txt")){
                /*
                byte bb[] = Base64Coder.decode(originalFileName);
                String zz="";
                try {
                    ss = new String(bb,"UTF-16");
                    } catch (UnsupportedEncodingException e) {
                        zz = "Не конвертируется\n";
                        }
                for (int i=0; i<bb.length;i++){
                    zz+=String.format("%x ",bb[i] & 0x0FF);
                    if ((i+1)%10==0)
                        zz+="\n";
                    }
                */
                return originalFileName + " - тип файла - не txt";
                }
            ss = ss.substring(0,ss.length()-4);
            int idx1=ss.indexOf("_");
            int idx2=ss.lastIndexOf("_");
            if (idx1==-1 || idx2==-1 || idx1==idx2)
                return originalFileName+": формат имени, нет \'_\'";
            lepNumber = ss.substring(idx2+1);
            srcNumber = ss.substring(idx1+1,idx2);
            ss = ss.substring(0,idx1);
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
            createDate = formatter.parseDateTime(ss);
            return null;
            }
            catch(Exception ee){
                return originalFileName+": "+ee.toString();
                }
            }
    public String toString(){
        return lepNumber+" "+srcNumber+"\n"+createDate.toString(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"));
        }
    public String measureMetaData(){
        return "Геолокация: "+gps.toString()+"\nГруппа-Опора: "+fileGroupTitle+"\nДата создания: "+fileDateTime+
                "\nЧастота: "+String.format("%6.2f",fileFreq)+"\nДатчик: "+fileSensorName+"\nНомер измерения: "+fileMeasureCounter;
        }
    public FileDescription(String fname){
        originalFileName = fname;
        }
    public static void main(String ss[]){
        FileDescription ff= new FileDescription("AAAAAAAAaaaaaaaa");
        System.out.println(ff.parseFromName());
        }
    }
