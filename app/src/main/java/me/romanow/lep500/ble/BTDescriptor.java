package me.romanow.lep500.ble;

public class BTDescriptor {
    public String btName="";    // Логическое имя сенсора
    public String btMAC="";     // MAC-адрес
    public BTDescriptor(String btName, String btMAC) {
        this.btName = btName;
        this.btMAC = btMAC;
        }
}
