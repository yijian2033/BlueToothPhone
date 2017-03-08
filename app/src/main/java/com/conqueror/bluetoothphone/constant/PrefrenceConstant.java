package com.conqueror.bluetoothphone.constant;

/**
 * Created by Administrator on 2017/2/7.
 */

public class PrefrenceConstant {


    public static final String SET_AUTO_LINK_KEY = "BtSetAutoLinkStateKey";//蓝牙自动连接
    public static final String SET_AUTO_ANSWER_KEY = "BtSetAutoAnswerKey";//自动接听电话
    public static final String SET_CLEAN_CACHE_KEY = "BtSetCleanCacheKey";//清空缓存联系人

    public static final String BT_DEVICE_NAME = "BtDeviceName";//连接的蓝牙设备名称
    public static final String SET_NATIVE_BT_NAME = "SetNativeBtName";//蓝牙本地名称

    public static final String BT_LINK_CUT_STATE = "BlueToothLinkOrCut";//蓝牙连接或者断开状态

    /**
     * 存储正在加载联系人的状态，true 表示正在加载，false表示加载完成
     */
    public static final String CONTACT_LOADING_STATE = "ContactLoadingState";

    public static final String PHONE_IS_WORKING = "PhoneIsWorking";//添加标记，以防通话中还有来电

    public static final String STORAGE_STATE = "StorageState";//存储拨打电话和通话状态

    public static final String BLUE_TOOTH_TELEPHONE_CALLING_NUMBER = "BlueTooth_Telephone_Calling_Number";//正在拨打的电话号码

    public static final String DEFAULT_BT_NAME_KEY = "DefaultBtName";//蓝牙第一次开机默认的名称

    public static final String PHONE_SUDDENLY_CUT = "TelephoneSuddenlyCutNumber";   //正在拨打电话突然断电时候

    /**
     * 蓝牙电源状态
     */
    public static final String BT_POWER_STATE = "BlueToothPowerState";

    public static final String CALLING_TELEPHONE_NUMBER = "CallingTelephoneNumber";//正在拨打的电话号码，存储，然后进去调用。
    public static final String RING_TELEPHONE_NUMBER = "RingTelephoneNumber";//来电的电话号码，存储，然后回显

}
