package com.conqueror.bluetoothphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.conqueror.bluetoothphone.bus.BusCancelLoadContact;
import com.conqueror.bluetoothphone.constant.BtReceiverAiosOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;

import org.greenrobot.eventbus.EventBus;

public class BtAiosReceiver extends BroadcastReceiver {
    private static final String TAG = BtAiosReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        LogUtil.showJohnLog(3, TAG + "--------BtAiosReceiver action----" + action);

        //收到语音请求蓝牙状态
        if (action.equals(BtReceiverAiosOrder.RECEIVER_AIOS_SATUS_REQUEST)) {
            int anInt = PreferenceUtils.getInt(context.getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT);
            switch (anInt) {
                case BtStates.BT_STATE_DEVICE_LINKED:
                    //发送已连接广播
                    context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_BT_CONNECTED));
                    break;
                case BtStates.BT_STATE_CUT:
                    //发送蓝牙断开广播
                    context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_BT_DISCONNECTED));
                    break;
            }
        }

        //如果是拨打电话的广播
        if (action.equals(BtReceiverAiosOrder.RECEIVER_AIOS_DIAL)) {

            int anInt = PreferenceUtils.getInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE);
            //如果是拨号状态，通话状态，来电状态
            if (anInt == BtStates.BT_STATE_PHONE_CALLING || anInt == BtStates.BT_STATE_PHONE_RUNNING || anInt == BtStates.BT_STATE_PHONE_TALKING) {
                //发送广播，正在拨号等待中
                LogUtil.showJohnLog(3, TAG + "-----send outgoing  running----");
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_RINGING));
                return;
            }
            //获取电话号码，拨打电话
            String number = intent.getStringExtra(BtToAIOSCastOrder.AIOS_NUMBER_KEY);

            LogUtil.showJohnLog(3, TAG + "----aios'number----" + number);

            BlueToothJniTool.getJniToolInstance(context).callPhone(number);

        }

        //如果是接听电话的广播
        if (action.equals(BtReceiverAiosOrder.RECEIVER_AIOS_ACCEPT)) {
            //接听电话
            BlueToothJniTool.getJniToolInstance(context).sendEasyCommand(JniConfigOder.BT_ANSWER);
            LogUtil.showJohnLog(3, TAG + "-------接听成功---------");
        }

        //如果是拒接电话
        if (action.equals(BtReceiverAiosOrder.RECEIVER_AIOS_REJECT)) {
            //拒接电话
            BlueToothJniTool.getJniToolInstance(context).sendEasyCommand(JniConfigOder.BT_REJECT);
            LogUtil.showJohnLog(3, TAG + "-------拒接电话---------");
        }

        if (action.equals(BtReceiverAiosOrder.AIOS_DISLOAD_CONTACT)) {
            ToastLoadContactUtil.getInstance().cancelDialog();
            //发给电话本界面，取消加载状态
            EventBus.getDefault().post(new BusCancelLoadContact());
        }
        if (action.equals(BtReceiverAiosOrder.AIOS_LOAD_CONTACT)) {
            ToastLoadContactUtil.getInstance().cancelDialog();
//            ToastLoadContactUtil.getInstance().saveContacts(context.getApplicationContext());
            /**存储正在加载联系人*/
            PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, true);
            BlueToothJniTool.getJniToolInstance(context).downloadPhoneBook();
        }

    }
}
