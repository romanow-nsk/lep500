package me.romanow.lep500;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileDescription {
    DateTime createDate = new DateTime();
    String lepNumber="";            // Номер опоры
    String srcNumber="";            // Номер датчика
    String comment="";              // Комментарий
    String originalFileName="";     // Оригинальное имя
    GPSPoint gps = new GPSPoint();
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
        return "Опора "+lepNumber+" датчик "+srcNumber+"\n"+createDate.toString(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"));
        }
    public FileDescription(String fname){
        originalFileName = fname;
        }
    public static void main(String ss[]){
        FileDescription ff= new FileDescription("AAAAAAAAaaaaaaaa");
        System.out.println(ff.parseFromName());
        }
    }
