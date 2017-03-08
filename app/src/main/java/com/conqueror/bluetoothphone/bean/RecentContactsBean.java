package com.conqueror.bluetoothphone.bean;

import org.litepal.crud.DataSupport;


public class RecentContactsBean extends DataSupport {


    private String number;
    private String numberName;
    private String numberType;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumberName() {
        return numberName;
    }

    public void setNumberName(String numberName) {
        this.numberName = numberName;
    }

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }

}
