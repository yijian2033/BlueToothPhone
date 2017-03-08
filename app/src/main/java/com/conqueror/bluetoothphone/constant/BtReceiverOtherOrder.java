package com.conqueror.bluetoothphone.constant;

/**
 * Created by Administrator on 2017/2/10.
 */

public class BtReceiverOtherOrder {

    public static final String ACC_PARKING = "com.conqueror.parkingMonitoring";//停车监控
    /**
     * 接收luncher的广播
     **/
    public static final String BLUETOOTH_ON = "com.conqueror.BLUETOOTH_ON";//开启蓝牙模块
    public static final String BLUETOOTH_OFF = "com.conqueror.BLUETOOTH_OFF";//关闭蓝牙模块

    /**
     * 发送给luncher的反馈
     **/
    public static final String BLUETOOTH_STATUSON = "com.conqueror.BLUETOOTH_STATUSON";//蓝牙模块开启通知
    public static final String BLUETOOTH_STATUSOFF = "com.conqueror.BLUETOOTH_STATUSOFF";//蓝牙模块关闭通知

    /**
     * 按键广播打开蓝牙
     */
    public static final String KEY_OPEN_BLUETOOTH = "com.conqueror.BlueTooth.ByKeyOpen";

    /**
     * 格式化发送的广播
     */
    public static final String FORMAT_TO_BLUETOOTHPHONE = "com.conqueror.BlueToothPhone.Format";

    // 真正进入停车监控
    public static final String ENTER_PARKING = "com.conqueror.acc.Action.EnterParking";
    // 真正取消停车监控
    public static final String NO_PARKING = "com.conqueror.acc.Action.NoParking";


}
