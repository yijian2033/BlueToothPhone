package com.conqueror.bluetoothphone.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bean.RecentContactsBean;
import com.conqueror.bluetoothphone.bus.BusCancelLoadContact;
import com.conqueror.bluetoothphone.bus.BusLoadContactCompleted;
import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.view.TimerDialog;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.util.ArrayList;


public class ToastLoadContactUtil {

    private static final String TAG = ToastLoadContactUtil.class.getName();

    private static ToastLoadContactUtil instance = null;
    private TimerDialog ldialog;

    public static ToastLoadContactUtil getInstance() {
        if (instance == null) {
            instance = new ToastLoadContactUtil();
        }
        return instance;
    }


    public void showToastContact(final Context context) {

        AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("是否加载联系人？");

        /**语音控制是否加载联系人*/
        context.sendBroadcast(new Intent(BtToAIOSCastOrder.AIOS_CONTROLL_LOAD_CONTACT));

        //弹出倒计时对话框
        ldialog = new TimerDialog(context);
        ldialog.setTitle("电话本没有号码，是否重新加载？");

        //确认按钮
        ldialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("联系人加载中");


                /**存储正在加载联系人*/
                PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, true);

                /***执行加载联系人的命令****/
                BlueToothJniTool.getJniToolInstance(context).downloadPhoneBook();

                ldialog.cancelDialog();
                //向语音发送已经加载联系人，取消语音控制
                context.sendBroadcast(new Intent(new Intent(BtToAIOSCastOrder.BT_LOAD_CANCEL_CONTACT_TO_AIOS)));
            }
        }, 15);

        //取消按钮
        ldialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("联系人未加载");
                ldialog.cancelDialog();

                //发送给电话本界面，取消加载联系人
                EventBus.getDefault().post(new BusCancelLoadContact());

                //向语音发送已经加载联系人，取消语音控制
                context.sendBroadcast(new Intent(new Intent(BtToAIOSCastOrder.BT_LOAD_CANCEL_CONTACT_TO_AIOS)));

            }
        }, 0);

        ldialog.show();
        ldialog.setButtonType(Dialog.BUTTON_POSITIVE, 15, true);
        ldialog.setButtonType(Dialog.BUTTON_NEGATIVE, 0, true);
    }


    public void cancelDialog() {
        if (ldialog != null) {
            ldialog.cancelDialog();
        }
    }
}
