package me.romanow.lep500;

import java.util.ArrayList;
import java.util.HashMap;

import me.romanow.lep500.ble.BTDescriptor;
import me.romanow.lep500.fft.FFT;


public class LEP500Settings {
    public double FirstFreq=0.4;               // Нижняя граница частоты при поиске максимусов
    public double LastFreq=30;                 // Верхняя граница частоты при поиске максимусов
    public int  nTrendPoints=50;               // Точек при сглаживании тренда =0 - отключено
    public int  p_BlockSize=1;                 // Количество блоков по 1024 отсчета
    public int  p_OverProc=50;                 // Процент перекрытия окна
    public int  kSmooth=30;                    // Циклов сглаживания
    public int  winFun= FFT.WinModeRectangle;   // Вид функции окна
    public int  measureDuration=10;            // Время снятия вибрации в сек (1...300)
    public String measureGroup="СМ-300";       // Подпись группы
    public String measureTitle="Опора 125";    // Подпись опоры
    public int measureCounter=1;               // Счетчик измерения
    public boolean fullInfo=false;
    public double measureFreq=102.8;            // Частота измерений
    public String mailToSend="romanow@ngs.ru";
    //----------------- Для javax.mail -------------------------------------------
    public transient String mailHost="mail.nstu.ru";
    public transient String mailBox="romanow@corp.nstu.ru";
    public transient String mailPass="";
    public transient String mailSecur="starttls";
    public transient int mailPort=587;
    public ArrayList<BTDescriptor> knownSensors=new ArrayList<>();
    //---------------------------------------------------------------------------
    public transient HashMap<String,BTDescriptor> nameMap = new HashMap<>();
    public transient HashMap<String,BTDescriptor> addressMap = new HashMap<>();
    public void createMaps(){
        nameMap.clear();
        addressMap.clear();
        for(BTDescriptor descriptor : knownSensors){
            nameMap.put(descriptor.btName,descriptor);
            addressMap.put(descriptor.btMAC,descriptor);
            }
        }
    public void removeByMAC(String macAddress){
        for(int i=0;i<knownSensors.size();i++){
            if (knownSensors.get(i).btMAC.equals(macAddress)){
                knownSensors.remove(i);
                createMaps();
                break;
            }

        }
        }
}
