package com.conqueror.bluetoothphone.constant;

/**
 * Created by Administrator on 2017/2/13.
 */

public class BtReceiverAiosOrder {
    /**
     * 语音或者按键关闭界面
     */
    public static final String AIOS_CLOSE_ACTIVITY = "com.aios.action.closeBlueToothPhone";

    /**
     * 语音请求蓝牙状态的广播
     */
    public static final String RECEIVER_AIOS_SATUS_REQUEST = "action.adapter.status.request";

    /**
     * 接收语音拨打电话的广播
     */
    public static final String RECEIVER_AIOS_DIAL = "action.intent.AIOS_DIAL";

    /**
     * 接收语音发来的接听广播
     */
    public static final String RECEIVER_AIOS_ACCEPT = "action.intent.AIOS_ACCEPT";

    /**
     * 接收拒绝来电的广播
     */
    public static final String RECEIVER_AIOS_REJECT = "action.intent.AIOS_REJECT";

    public static final String AIOS_LOAD_CONTACT = "com.conquer.aios.bt.loadContact";//语音确定加载联系人
    public static final String AIOS_DISLOAD_CONTACT = "com.conquer.aios.bt.cancelLoadContact";//语音取消加载联系人
}
