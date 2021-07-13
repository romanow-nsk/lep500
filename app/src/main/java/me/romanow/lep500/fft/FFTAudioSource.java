/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.romanow.lep500.fft;

import java.io.IOException;

/**
 *
 * @author romanow
 */
public interface FFTAudioSource {
    public String testSource(int sizeHZ);
    public long getFrameLength();
    public int read(double buf[], int offset, int lnt) throws IOException;
    public void close() throws IOException;
    public double getSampleRate();
}
