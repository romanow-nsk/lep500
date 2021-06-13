/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;


/**
 *
 * @author romanow
 */
public class FFTParams {
    private int W=1024;                    // Ширина окна (отсчетов)
    private double freqHZ=100;              // Частота оцифровки
    private int procOver=75;               // Процент перекрытия двух соседних окон
    private double  F_SCALE=3.0;           // Коэффициент перемножения спектр*гамматон
    private int winMode=FFT.WinModeRectangle; // Вид функции окна
    private double compressGrade=0;         // Степень компрессии
    private boolean compressMode=false;
    private double kAmpl=1;                 // Ампл. компрессии
    public FFTParams procOver(int procOver0){
        procOver = procOver0;
        return this;
        }
    public FFTParams W(int W0){
        W = W0;
        return this;
        }
    public FFTParams compressMode(boolean compressMode0){
        compressMode = compressMode0;
        return this;
        }
    public FFTParams compressGrade(float compressGrade0){
        compressGrade = compressGrade0;
        return this;
        }
    public FFTParams f_SCALE(double f_SCALE0){
        F_SCALE = f_SCALE0;
        return this;
        }
    public FFTParams kAmpl(float kAmpl0){
        kAmpl = kAmpl0;
        return this;
        }
    public FFTParams winMode(int winMode0){
        winMode = winMode0;
        return this;
        }
    public double kAmpl(){ return kAmpl; }
    public int winMode(){
        return winMode;
        }
    public String toString(){
        return "Ширина окна="+W+"\nПроцент перекрытия="+procOver+
                "\nКомпрессия "+(compressMode ? ("+\nУровень="+compressGrade+"\nАмплитуда="+kAmpl) : "-");
        }
    public FFTParams freqHZ(double freqHZ0){
        freqHZ = freqHZ0;
        return this;
        }
    public double freqHZ(){
        return freqHZ; }
    public double compressGrade() {
        return compressGrade; }
    public boolean compressMode() {
        return compressMode; }
    public double F_SCALE() {
        return F_SCALE;
        }
    public FFTParams(){}

    public int W() {
        return W; }
    public int procOver() {
        return procOver; }
    }
