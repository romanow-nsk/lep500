package me.romanow.lep500;

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
}
