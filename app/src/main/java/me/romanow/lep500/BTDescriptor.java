package me.romanow.lep500;

public class BTDescriptor {
    final public String btName;    // Логическое имя сенсора
    final public String btMAC;     // MAC-адрес
    public BTDescriptor(String btName, String btMAC) {
        this.btName = btName;
        this.btMAC = btMAC;
    }
}
