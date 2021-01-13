package me.romanow.lep500;

public class LEP500Settings {
    double FirstFreq=0.4;       // Частоты отображения графика
    double LastFreq=30;
    int  nTrendPoints=50;       // Точек при сглаживании тренда =0 - отключено
    int  p_BlockSize=1;
    int  p_OverProc=50;
    int  kSmooth=30;             // Циклов сглаживания
}
