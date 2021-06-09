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
public class TimeCounter {
    private String names[]=null;
    private int tt[]=null;
    private long tc=0;
    private long fix=0;
    public void clear(){
        for(int i=0;i<tt.length;i++)
            tt[i]=0;
        fixTime();
        }
    public TimeCounter(String names[]){
        this.names = names;
        tt = new int[names.length];
        clear();
        }
    public TimeCounter(String name){
        this.names = new String[1];
        names[0] = name;
        tt = new int[1];
        clear();
        }
    public void fixTime(){
        fix = tc = System.currentTimeMillis();
        }
    public int getTotal(){
        return (int)(System.currentTimeMillis()-fix);
        }
    public void addCount(int idx){
        long t0 = System.currentTimeMillis();
        tt[idx] += (int)(t0-tc);
        tc = t0;
        }
    public void addCountTotal(int idx){
        tt[idx] += getTotal();
        }
    public String toString(){
        String out = "";
        for(int i=0;i<names.length;i++)
            out += names[i]+" "+tt[i]+" мс\n";
        return out;
        }
}
