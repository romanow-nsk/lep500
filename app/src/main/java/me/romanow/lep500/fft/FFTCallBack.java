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
public interface FFTCallBack extends I_Notify {
    public void onStart(double stepMS);
    public void onFinish();
    public boolean onStep(int nBlock, int calcMS, double totalMS, FFT fft);
    }
