/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;


import me.romanow.lep500.FileDescription;

/**
 *
 * @author romanow
 */
public interface FFTFileSource extends FFTAudioSource{      // маркерный интерфейс
    public boolean testAndOpenFile(FileDescription fd,int mode, String PatnToFile, int sizeHZ, FFTCallBack back);
    public String getFileSpec();
}
