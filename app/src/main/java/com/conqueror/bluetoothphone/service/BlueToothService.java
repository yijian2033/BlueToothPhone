package com.conqueror.bluetoothphone.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;

import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bean.RecentContactsBean;
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusJniNumber;
import com.conqueror.bluetoothphone.bus.BusLoadContactCompleted;
import com.conqueror.bluetoothphone.bus.BusNativeBtName;
import com.conqueror.bluetoothphone.bus.BusNotifyRecentBook;
import com.conqueror.bluetoothphone.bus.BusPhoneBookData;
import com.conqueror.bluetoothphone.bus.BusPhoneBtName;
import com.conqueror.bluetoothphone.constant.BtDefaultValue;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;
import com.conqueror.bluetoothphone.constant.BtToOtherCastOrder;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.AudioSetAndManager;
import com.conqueror.bluetoothphone.util.AIOSTTSpeakUtil;
import com.conqueror.bluetoothphone.util.ContactUtil;
import com.conqueror.bluetoothphone.util.GetTelephoneNumber;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 监听蓝牙状态的服务
 */
public class BlueToothService extends Service {

    private static final String TAG = BlueToothService.class.getName();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.showJohnLog(3, TAG + "--开机启动蓝牙服务----------");

        //注册EventBus
        EventBus.getDefault().register(this);

        //创建数据库表
        Connector.getDatabase();


        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {

                try {
//                    Thread.sleep(1000);

                    //给蓝牙模块供电
                    BlueToothJniTool.getJniToolInstance(getApplicationContext()).powerControl(1);

                    //启动状态查询
                    BlueToothJniTool.getJniToolInstance(getApplicationContext()).getCurrentState();

                    Thread.sleep(1000);
                    LogUtil.showJohnLog(3, TAG + "-----发送获取本地蓝牙名称-----");
                    //获取蓝牙本地名称,会以广播的形式发送出来
                    BlueToothJniTool.getJniToolInstance(getApplicationContext()).getLocalBluetoothName();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });


        //发送给luncher蓝牙已经启动
        sendBroadcast(new Intent(BtToOtherCastOrder.BLUETOOTH_STATUSON));


    }


    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    /**
     * 正在拨打的电话号码
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(BusJniNumber event) {
        if (BtStates.BT_STATE_DEVICE_LINKED
                == PreferenceUtils.getInt(getApplicationContext()
                , PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT)) {
            saveCallNumber(getApplicationContext(), event.jniNumber);
        }
    }

    /**
     * 蓝牙状态
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(BusJniBtState event) {
        getBlueToothState(getApplicationContext(), event.btState);

    }


    /**
     * 连接的蓝牙名称
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusPhoneBtName event) {
        //先判断蓝牙是否连接
        if (BtStates.BT_STATE_DEVICE_LINKED ==
                PreferenceUtils.getInt(getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT)) {
            linkBtName(getApplicationContext(), event.phoneName);
        }
    }

    /**
     * 本地的蓝牙名称
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(BusNativeBtName event) {

        if (event.nativeBtName == null) {
            return;
        }

        String stringSaveName = PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, null);
        LogUtil.showJohnLog(3, TAG + "--本地蓝牙名称-------" + stringSaveName);

        //初始化本地蓝牙名称
        if (event.nativeBtName.equals(BtDefaultValue.BT_DEFAULT_NAME)) {
            String deviceBtName = "BS360";
            Random random = new Random();
            int i = random.nextInt(1000);
            if (i >= 0 && i < 10) {
                deviceBtName = deviceBtName + "_00" + i;
            } else if (i >= 10 && i < 100) {
                deviceBtName = deviceBtName + "_0" + i;
            } else {
                deviceBtName = deviceBtName + "_" + i;
            }

            LogUtil.showJohnLog(3, TAG + "----首次启动，设置本地的蓝牙名称 -----" + deviceBtName);

            //存储首次设置的名称，以便于格式化后恢复本次名称
            PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.DEFAULT_BT_NAME_KEY, deviceBtName);

            //存储新名称
            PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, deviceBtName);
        } else {
            LogUtil.showJohnLog(3, TAG + "--已经存蓝牙的本地名称-------" + event.nativeBtName);
            PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, event.nativeBtName);
        }

        BlueToothJniTool.getJniToolInstance(getApplicationContext()).setBluetoothName(PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, BtDefaultValue.BT_DEFAULT_NAME + "_000"));


    }


    /**
     * 加载电话本
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(BusPhoneBookData event) {
        //先判断蓝牙是否连接
        if (BtStates.BT_STATE_DEVICE_LINKED ==
                PreferenceUtils.getInt(getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT)) {

            saveContacts(getApplicationContext(), event.phoneBook);
        }
    }


    private void saveCallNumber(Context context, String jniNumber) {

        //存储拨打的电话号码
        PreferenceUtils.putString(context.getApplicationContext(), PrefrenceConstant.BLUE_TOOTH_TELEPHONE_CALLING_NUMBER, jniNumber);

        //存储拨号联系人

        final String callingNumber = GetTelephoneNumber.getRingingNumber(jniNumber);

        if (callingNumber.equals("未知号码")) {
            RecentContactsBean recentContactsBean = new RecentContactsBean();
            recentContactsBean.setNumberName("未知联系人");
            recentContactsBean.setNumber("");
            recentContactsBean.save();
        } else {
            //从数据库中查询电话号码的名称
            List<ContactBean> contactBeen = DataSupport.where("phoneNumber=?", callingNumber).find(ContactBean.class);
            if (contactBeen.size() == 0) {
                RecentContactsBean recentContactsBean = new RecentContactsBean();
                recentContactsBean.setNumberName("未知");
                recentContactsBean.setNumber(callingNumber);
                recentContactsBean.save();
            } else {
                ContactBean cBean = contactBeen.get(0);
                RecentContactsBean recentContactsBean = new RecentContactsBean();
                recentContactsBean.setNumberName(cBean.getName());
                recentContactsBean.setNumber(callingNumber);
                recentContactsBean.save();
            }
        }

        EventBus.getDefault().post(new BusNotifyRecentBook());

    }


    /**
     * 蓝牙状态的判定和执行
     *
     * @param context
     * @param blueToothState
     */
    private void getBlueToothState(Context context, int blueToothState) {

        switch (blueToothState) {


            case BtStates.BT_STATE_PHONE_RUNNING://来电状态

                //存储状态
                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, blueToothState);

                //启动获取正在拨打号码的线程；这里不需要，因为响铃的时候自动读取了一次
                BlueToothJniTool.getJniToolInstance(context).recentCalledNumber();


                //如果是自动接听电话
                if (PreferenceUtils.getBoolean(context.getApplicationContext(), PrefrenceConstant.SET_AUTO_ANSWER_KEY, false)) {

                    PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.PHONE_IS_WORKING, true);
                    Intent toTalk = new Intent(context, TalkingService.class);
                    context.startService(toTalk);
                    BlueToothJniTool.getJniToolInstance(context).sendEasyCommand(JniConfigOder.BT_ANSWER);//接听电话
                } else {
                    Intent toRing = new Intent(context, RingService.class);
                    context.startService(toRing);
                }

                break;


            case BtStates.BT_STATE_PHONE_CALLING://拨打电话状态

                //获取拨打的电话号码
                BlueToothJniTool.getJniToolInstance(context).recentCalledNumber();

                AudioManager mAm = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int streamVolume = mAm.getStreamVolume(AudioManager.STREAM_MUSIC);
                LogUtil.showJohnLog(3, TAG + "----receiver ----系统 voice 大小-------" + streamVolume);

                //存储状态
                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, blueToothState);

                //打开callingService
                context.startService(new Intent(context.getApplicationContext(), CallingService.class));

                break;


            case BtStates.BT_STATE_PHONE_TALKING://通话状态

                /**为了防止多次启动，通话界面**/
                if (PreferenceUtils.getBoolean(context.getApplicationContext(), PrefrenceConstant.PHONE_IS_WORKING, false)) {
                    return;
                }
                context.stopService(new Intent(context, CallingService.class));
                context.stopService(new Intent(context, RingService.class));

                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, blueToothState);

                Intent toTalk = new Intent(context, TalkingService.class);
                context.startService(toTalk);

                //发送广播给语音
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.CALL_ACCEPT));

                PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.PHONE_IS_WORKING, true);

                break;


            case BtStates.BT_STATE_PHONE_TALK_END://通话结束状态

                //添加标记，以防通话中还有来电
                PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.PHONE_IS_WORKING, false);

                //停止所有的服务
                context.stopService(new Intent(context, CallingService.class));
                context.stopService(new Intent(context, TalkingService.class));
                context.stopService(new Intent(context, RingService.class));

                //释放音频焦点
                AudioSetAndManager.getInstance(context).startMusic();

                PreferenceUtils.putInt(context.getApplicationContext(), PrefrenceConstant.STORAGE_STATE, blueToothState);

                LogUtil.showJohnLog(3, TAG + "----收到通话结束状态------");

                /**向语音发送挂断电话的广播**为了防止蓝牙突然中断而不能，而不能唤醒语音*/
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_IDLE));
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_INCOMING_IDLE));
                break;


            case BtStates.BT_STATE_CUT://蓝牙断开状态

                //停止所有的服务
                context.stopService(new Intent(context, CallingService.class));
                context.stopService(new Intent(context, TalkingService.class));
                context.stopService(new Intent(context, RingService.class));

                //存储蓝牙断开状态
                PreferenceUtils.putInt(context, PrefrenceConstant.BT_LINK_CUT_STATE, blueToothState);

                //取消对话框
                ToastLoadContactUtil.getInstance().cancelDialog();

                //断开蓝牙后是否清空缓存
                boolean isCleanContact = PreferenceUtils.getBoolean(context.getApplicationContext(), PrefrenceConstant.SET_CLEAN_CACHE_KEY, true);

                List<ContactBean> beens = DataSupport.findAll(ContactBean.class);

                if ((!isCleanContact) && (beens.size() > 0)) {//清空联系人
                    LogUtil.showJohnLog(3, TAG + "------clean contacts----");
                    AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("蓝牙断开，联系人已清空");
                    DataSupport.deleteAll(ContactBean.class);
                } else {
                    AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("蓝牙断开");
                }

                /**给语音发送蓝牙断开广播*/
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_BT_DISCONNECTED));

                //如果为不是自动连接false,取消自动连接,做两个判断条件是为了避免按开关的时候断开蓝牙
                if (!PreferenceUtils.getBoolean(context.getApplicationContext(), PrefrenceConstant.SET_AUTO_LINK_KEY)) {
                    BlueToothJniTool.getJniToolInstance(context).sendEasyCommand(JniConfigOder.BT_CANCEL_AUTO_LINK);
                }

                /**向语音发送挂断电话的广播**为了防止蓝牙突然中断而不能，而不能唤醒语音*/
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_IDLE));
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_INCOMING_IDLE));

                break;

            case BtStates.BT_STATE_DEVICE_LINKED://蓝牙设备连接状态
                //存储蓝牙连接状态
                PreferenceUtils.putInt(context, PrefrenceConstant.BT_LINK_CUT_STATE, blueToothState);

                AIOSTTSpeakUtil.getInstance(getApplicationContext()).sendTTSpeak("蓝牙连接");

                //给语音发送蓝牙连接广播
                context.sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_BT_CONNECTED));
                break;
        }

    }

    /**
     * 连接的蓝牙名称
     *
     * @param context
     * @param tellBluetoothName
     */
    private void linkBtName(Context context, String tellBluetoothName) {

        //以前存储的蓝牙名称
        String getSaveBluetoothName = PreferenceUtils.getString(context, PrefrenceConstant.BT_DEVICE_NAME, "未知");

        //储存蓝牙名称，判断所获取蓝牙名称是否为空，如果为空就添 已连接
        PreferenceUtils.putString(context, PrefrenceConstant.BT_DEVICE_NAME, tellBluetoothName == null ? BtDefaultValue.DEFAULT_LINK_PHONE_BT_NAME : tellBluetoothName);

        //如果联系人为空，就必须加载数据
        List<ContactBean> all = DataSupport.findAll(ContactBean.class);
        LogUtil.showJohnLog(3, TAG + "-------contact size-----" + all.size() + '\n' + "----getSaveName-------" + getSaveBluetoothName + '\n' + "---tellBTName------" + tellBluetoothName);

        if (!getSaveBluetoothName.equals(tellBluetoothName)) {
            //名字不一样就删除最近联系人
            DataSupport.deleteAll(RecentContactsBean.class);
        }
        //联系人是否已经缓存
        if (all.size() == 0 || !getSaveBluetoothName.equals(tellBluetoothName)) {
            /***  清除先前的缓存  ***/
            DataSupport.deleteAll(ContactBean.class);
            ToastLoadContactUtil.getInstance().showToastContact(context);
        } else {
            AIOSTTSpeakUtil.getInstance(context).sendTTSpeak("联系人已加载");
        }
    }


    /**
     * 将联系人存储起来，并发送广播给AIOS
     */
    public void saveContacts(final Context context, final String books) {

        if (books == null) {
            LogUtil.showJohnLog(3, TAG + "-------导入联系人为空------");
            return;
        }

        DataSupport.deleteAll(ContactBean.class);

        DataSupport.deleteAll(RecentContactsBean.class);

        /**存储正在加载联系人*/
        PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, true);

        //使用线程池管理线程
        ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
            @Override
            public void run() {
                StringBuffer btName = new StringBuffer();
                StringBuffer btNumber = new StringBuffer();
                //获取联系人
                LogUtil.showJohnLog(3, TAG + "---get contact number --books--length-------" + books.length() + "\n" + "--------phoneBooks------" + books);

                String replaceBooks = books.replace("�", "");

                ArrayList<ContactBean> contacts = ContactUtil.getContacts(replaceBooks);

                //存储到数据库当中
                DataSupport.saveAll(contacts);

                //将名字和电话分开来发送给语音
                for (int i = 0; i < contacts.size(); i++) {
                    ContactBean contactBean = contacts.get(i);
                    btName.append(contactBean.getName() + ",");
                    btNumber.append(contactBean.getPhoneNumber() + ",");
                    if (i == (contacts.size() - 1)) {
                        btName.append(contactBean.getName());
                        btNumber.append(contactBean.getPhoneNumber());
                    }
                }

                if (contacts.size() == 0) {
                    AIOSTTSpeakUtil.getInstance(getApplicationContext()).sendTTSpeak("联系人加载失败,请在手机蓝牙中检查是否授权导入联系人");
                } else {
                    AIOSTTSpeakUtil.getInstance(getApplicationContext()).sendTTSpeak("联系人加载完成");
                    //发送给电话本界面，取消加载联系人
                    EventBus.getDefault().post(new BusLoadContactCompleted());
                }


                // 联系人加载完成之后，在发送
                if (contacts.size() > 0) {
                    Intent intent = new Intent(BtToAIOSCastOrder.PHONELIST_TO_AIOS);
                    intent.putExtra(BtToAIOSCastOrder.PHONELIST_TO_AIOS_NAME_KEY, btName.toString());
                    intent.putExtra(BtToAIOSCastOrder.PHONELIST_TO_AIOS_NUMBER_KEY, btNumber.toString());
                    context.sendBroadcast(intent);
                }

                /**存储联系人加载完成*/
                PreferenceUtils.putBoolean(context.getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false);
            }
        });
    }


}
