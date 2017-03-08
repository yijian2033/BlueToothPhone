package com.conqueror.bluetoothphone.bus;

import android.content.Context;

import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bean.RecentContactsBean;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.util.GetTelephoneNumber;
import com.conqueror.bluetoothphone.util.PreferenceUtils;

import org.litepal.crud.DataSupport;

import java.util.List;

/**
 * 来去电获取的电话号码
 */

public class BusJniNumber {
    public String jniNumber;

    public BusJniNumber(String jniNumber) {
        this.jniNumber = jniNumber;
    }

}
