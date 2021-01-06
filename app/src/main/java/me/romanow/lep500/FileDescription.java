package me.romanow.lep500;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileDescription {
    long createData = 0;            // Дата создания
    String lepNumber="";            // Номер опоры
    String srcNumber="";            // Номер датчика
    String comment="";              // Комментарий
    String originalFileName="";     // Оригинальное имя
    GPSPoint gps = new GPSPoint();
    public String parseFromName(){
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
        SimpleDateFormat format = new SimpleDateFormat("YYYYMMdd'T'HHmmss");
        Date dd = format.parse(ss,new ParsePosition(0));
        createData = dd.getTime();
        String zz = format.format(dd);
        return null;
        }
    public String toString(){
        return "Опора "+lepNumber+" датчик "+srcNumber+"\n"+new SimpleDateFormat("dd-MM-YYYY HH:mm:ss").format(new Date(createData));
        }
    public FileDescription(String fname){
        originalFileName = fname;
        }
    }
