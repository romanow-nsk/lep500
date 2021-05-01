package me.romanow.lep500;

import java.util.ArrayList;
import java.util.HashMap;

import romanow.snn_simulator.fft.FFT;

public class LEP500Settings {
    double FirstFreq=0.4;               // Нижняя граница частоты при поиске максимусов
    double LastFreq=30;                 // Верхняя граница частоты при поиске максимусов
    int  nTrendPoints=50;               // Точек при сглаживании тренда =0 - отключено
    int  p_BlockSize=1;                 // Количество блоков по 1024 отсчета
    int  p_OverProc=50;                 // Процент перекрытия окна
    int  kSmooth=30;                    // Циклов сглаживания
    int  winFun=FFT.WinModeRectangle;   // Вид функции окна
    int  measureDuration=10;            // Время снятия вибрации в сек (1...300)
    String measureGroup="СМ-300";       // Подпись группы
    String measureTitle="Опора 125";    // Подпись опоры
    int measureCounter=1;               // Счетчик измерения
    boolean fullInfo=false;
    ArrayList<BTDescriptor> knownSensors=new ArrayList<>();
    //---------------------------------------------------------------------------
    transient HashMap<String,BTDescriptor> nameMap = new HashMap<>();
    transient HashMap<String,BTDescriptor> addressMap = new HashMap<>();
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
