package com.conqueror.bluetoothphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.ThreadPoolProxy;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;

import org.greenrobot.eventbus.EventBus;

public class BtLuncherReceiver extends BroadcastReceiver {

    private static final String TAG = BtLuncherReceiver.class.getName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        LogUtil.showJohnLog(3, TAG + "----------luncherReceiver-----action--" + action);

        if (action.equals(BtReceiverOtherOrder.BLUETOOTH_ON)) {//打开蓝牙
            openBT(context, false);
        } else if (action.equals(BtReceiverOtherOrder.BLUETOOTH_OFF)) {
            closeBT(context, false);
        }

        if (action.equals(BtReceiverOtherOrder.ENTER_PARKING)) {//进入停车监控
            //关闭一切
            closeBT(context, true);
        } else if (action.equals(BtReceiverOtherOrder.NO_PARKING)) {//退出停车监控
            //打开一切
            openBT(context, true);
        }

    }

    private void closeBT(final Context context, final boolean isAcc) {
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {
                //开机之后启动蓝牙模块,发送给luncher
                LogUtil.showJohnLog(3, TAG + "------close bt power-----");
                //关闭电源
                BlueToothJniTool.getJniToolInstance(context.getApplicationContext()).powerControl(0);
                //给luncher的反馈
                context.sendBroadcast(new Intent(BtReceiverOtherOrder.BLUETOOTH_STATUSOFF));

                //蓝牙的连接状态，默认为断开
                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT);

                //蓝牙的电话状态为结束通话状态
                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, BtStates.BT_STATE_PHONE_TALK_END);

                //正在拨打电话突然断电时候
                PreferenceUtils.putString(context.getApplicationContext(), PrefrenceConstant.PHONE_SUDDENLY_CUT, "");

                //蓝牙电源为关闭状态
                PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.BT_POWER_STATE, false);

                //给设置界面发送蓝牙断开广播
//                EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_CUT));

                EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_PHONE_TALK_END));

                if (isAcc) { //关闭串口
                    LogUtil.showJohnLog(3, TAG + "---关闭串口--");
                    BlueToothJniTool.getJniToolInstance(context.getApplicationContext()).closeBT();
                }
            }
        });
    }

    private void openBT(final Context context, final boolean isNoAcc) {
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {

                //打开串口
                if (isNoAcc) {

                    LogUtil.showJohnLog(3, TAG + "---打开串口--");
                    BlueToothJniTool.getJniToolInstance(context.getApplicationContext()).openBT();

                    //1表示开，0表示关
                    BlueToothJniTool.getJniToolInstance(context.getApplicationContext()).powerControl(1);
                    LogUtil.showJohnLog(3, TAG + "---ACC 打开-BT 电源-");

                    context.sendBroadcast(new Intent(BtReceiverOtherOrder.BLUETOOTH_STATUSON));
                    PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.BT_POWER_STATE, true);

                } else {
                    //1表示开，0表示关
                    BlueToothJniTool.getJniToolInstance(context.getApplicationContext()).powerControl(1);
                    context.sendBroadcast(new Intent(BtReceiverOtherOrder.BLUETOOTH_STATUSON));

                    PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.BT_POWER_STATE, true);

                }
            }
        });
    }
}
