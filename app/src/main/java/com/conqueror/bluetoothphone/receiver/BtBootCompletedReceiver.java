package com.conqueror.bluetoothphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.conqueror.bluetoothphone.activity.MainActivity;
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.constant.AppPackageName;
import com.conqueror.bluetoothphone.constant.BtReceiverAiosOrder;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.service.BlueToothService;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;

import org.greenrobot.eventbus.EventBus;

/**
 * 开机启动服务
 */
public class BtBootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = BtBootCompletedReceiver.class.getName();
    private static final String AIOS_REBOOT_COMPELETED = "com.bs360.notify_fm_and_bt_boot";


    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        LogUtil.showJohnLog(3, TAG + "--------BtBootCompletedReceiver----------" + action);

        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {//开机启动

            Intent newIntent = new Intent(context, BlueToothService.class);
            context.startService(newIntent);
            //发送给语音
            context.sendBroadcast(new Intent("com.conqueror.action.jw.CompleteBoot"));

            //蓝牙的连接状态，默认为断开
            PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT);

            //蓝牙的电话状态为结束通话状态
            PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, BtStates.BT_STATE_PHONE_TALK_END);

            //正在拨打电话突然断电时候
            PreferenceUtils.putString(context.getApplicationContext(), PrefrenceConstant.PHONE_SUDDENLY_CUT, "");


        }
        if (action.equals(BtReceiverOtherOrder.KEY_OPEN_BLUETOOTH)) {//按键广播，是开就关，是关就开

            boolean mapForground = AppUtil.getInstance(context).isMapForground(context, AppPackageName.BLUETOOTH_APP);

            if (mapForground) {
                context.sendBroadcast(new Intent(BtReceiverAiosOrder.AIOS_CLOSE_ACTIVITY));
            } else {
                Intent toMain = new Intent(context, MainActivity.class);
                toMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(toMain);
            }

        }
        //收到格式化的广播
        if (action.equals(BtReceiverOtherOrder.FORMAT_TO_BLUETOOTHPHONE)) {

            String btName = PreferenceUtils.getString(context.getApplicationContext(), PrefrenceConstant.DEFAULT_BT_NAME_KEY);

            LogUtil.showJohnLog(3, TAG + "----收到格式化广播---蓝牙的名称----" + btName);

            //重新设置蓝牙名称
            PreferenceUtils.putString(context.getApplicationContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, btName);
        }
    }
}
