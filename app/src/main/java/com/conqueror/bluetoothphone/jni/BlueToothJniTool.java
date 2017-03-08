package com.conqueror.bluetoothphone.jni;


import android.content.Context;

import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusJniNumber;
import com.conqueror.bluetoothphone.bus.BusNativeBtName;

import com.conqueror.bluetoothphone.bus.BusPhoneBookData;
import com.conqueror.bluetoothphone.bus.BusPhoneBtName;

import com.conqueror.bluetoothphone.util.LogUtil;


import org.greenrobot.eventbus.EventBus;

public class BlueToothJniTool {

    private static final String TAG = BlueToothJniTool.class.getName();
    public static BlueToothJniTool tool = null;
    private static Context mContext;

    private BlueToothJniTool(Context context) {
//        this.mContext = context;
    }

    public static BlueToothJniTool getJniToolInstance(Context context) {
        mContext = context;
        if (null == tool) {
            tool = new BlueToothJniTool(context);
        }
        return tool;
    }

    static {
        System.loadLibrary("BlueToothJni");
    }


    public void displayMessage(int codeValue) {
        LogUtil.showJohnLog(3, TAG + "-------get jni state -------" + codeValue);
        EventBus.getDefault().post(new BusJniBtState(codeValue));
    }

    public void sendTelNumber(String numberValue) {
        LogUtil.showJohnLog(3, TAG + "-------get jni call or ring number -------" + numberValue);
        if (numberValue != null) {
            EventBus.getDefault().post(new BusJniNumber(numberValue));
        }
    }


    public void sendPhoneNameData(String phoneName) {
        LogUtil.showJohnLog(3, TAG + "-------match phone BT name-------" + phoneName);
        if (phoneName != null) {
            EventBus.getDefault().post(new BusPhoneBtName(phoneName));
        }
    }

    public void sendBtNameData(String nativeBtName) {
        LogUtil.showJohnLog(3, TAG + "-------get native bt name -------" + nativeBtName);
        if (nativeBtName != null) {
            EventBus.getDefault().post(new BusNativeBtName(nativeBtName));
        }
    }

    public void sendPhoneBookData(String phoneBook) {
        LogUtil.showJohnLog(3, TAG + "-------get jni  phone book -------" + phoneBook);
        if (phoneBook != null) {
            EventBus.getDefault().post(new BusPhoneBookData(phoneBook));
        }
    }


    public native int sendEasyCommand(int cmd);

    public native void callPhone(String number);

    public native int getCurrentState();

    public native void downloadPhoneBook();

    public native String recentCalledNumber();

    public native void setBluetoothName(String bluetoothName);

    public native String getTellBluetoothName();

    public native void getLocalBluetoothName();

    public native void setPIN(String pin);

    public native int getPIN();

    public native int cancelBtAutoLink();

    public native void switchVoice();

    public native void powerControl(int i);

    public native void openBT();

    public native void closeBT();

}
