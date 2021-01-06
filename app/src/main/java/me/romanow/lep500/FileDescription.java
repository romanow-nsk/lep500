package me.romanow.lep500;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
        String ss = originalFileName.toUpperCase();
        if (!ss.endsWith(".TXT"))
            return "Тип файла - не txt";
        ss = ss.substring(0,ss.length()-4);
        int idx1=ss.indexOf("_");
        int idx2=ss.lastIndexOf("_");
        if (idx1==-1 || idx2==-1 || idx1==idx2)
            return "Ошибка формата имени, нет \'_\'";
        lepNumber = ss.substring(idx2+1);
        srcNumber = ss.substring(idx1+1,idx2);
        ss = ss.substring(0,idx1);
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
        createDate = formatter.parseDateTime(ss);
        return null;
        }
    public String toString(){
        return "Опора "+lepNumber+" датчик "+srcNumber+"\n"+createDate.toString(DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss"));
        }
    public FileDescription(String fname){
        originalFileName = fname;
        }
    }
