package com.conqueror.bluetoothphone.constant;

/**
 * Created by Administrator on 2017/2/9.
 */

public class BtToAIOSCastOrder {

    public static final String AIOS_CONTROLL_LOAD_CONTACT = "com.conquer.bt.LoadOrCancelContact";//语音是否加载联系人
    public static final String BT_LOAD_CANCEL_CONTACT_TO_AIOS = "com.conqueror.bt.aios.LoadOrCancelContact";//蓝牙按键确认或者取消加载联系人发送给语音

    public static final String PHONELIST_TO_AIOS = "com.AnywheeBt.Synchronous_PhoneList_Aios";//联系人加载完成之后发送给语音
    public static final String PHONELIST_TO_AIOS_NAME_KEY = "BTName";//姓名KEY
    public static final String PHONELIST_TO_AIOS_NUMBER_KEY = "BTNum";//电话号码KEY

    public static final String AIOS_TTS_SPEAK = "com.aispeech.aios.adapter.speak";//发送TTS语音播报广播


    public static final String SEND_AIOS_BT_CONNECTED = "com.android.bt.connected";//向语音发送蓝牙已连接状态
    public static final String SEND_AIOS_BT_DISCONNECTED = "com.android.bt.disconnected";//向语音发送蓝牙断开状态


    public static final String SEND_AIOS_OUTGOING_IDLE = "action.bt.AIOS_OUTGOING_IDLE";// 对方没有接听，或者挂断电话，向语音发送通话结束
    public static final String SEND_AIOS_INCOMING_IDLE = "action.bt.AIOS_INCOMING_IDLE";// 挂断电话发送广播


    public static final String CALL_ACCEPT = "com.conqueror.bluetootphone.acceptCall";//发送电话已接听的广播

    /**
     * 向语音发送正在拨打电话状态
     */
    public static final String SEND_AIOS_OUTGOING_RINGING = "action.bt.AIOS_OUTGOING_RINGING";// 向语音发送正在拨打电话状态


    /**
     * 对方接听后，向语音发送正在通话中
     */
    public static final String SEND_AIOS_OUTGOING_OFFHOOK = "action.bt.AIOS_OUTGOING_OFFHOOK";

    /**
     * 向语音发送收到来电的广播
     */
    public static final String SEND_AIOS_INCOMING_RINGING = "action.bt.AIOS_INCOMING_RINGING";

    /**
     * 获取号码的KEY
     */
    public static final String AIOS_NUMBER_KEY = "number";

    /**
     * 获取姓名的KEY
     */
    public static final String AIOS_NAME_KEY = "name";



}
