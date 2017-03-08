package com.conqueror.bluetoothphone.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.bean.ContactBean;

import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusJniNumber;
import com.conqueror.bluetoothphone.constant.AppPackageName;
import com.conqueror.bluetoothphone.constant.BtDefaultValue;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.AudioSetAndManager;
import com.conqueror.bluetoothphone.manager.RecoredManager;

import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.GetTelephoneNumber;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.view.BackLinearLayout;
import com.conqueror.bluetoothphone.view.NoDoubleClickListener;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CallingService extends Service implements View.OnClickListener {

    private static final String TAG = CallingService.class.getName();

    private WindowManager mWindow;
    private LayoutInflater inflater;
    private WindowManager.LayoutParams params;
    private BackLinearLayout mInflateView;
    private ImageButton mIbCallSwitchVoice;
    private ImageButton mIbCallHungUp;
    private ImageButton mIbGoDials;
    private TextView mTvCallName;

    private Timer timer;
    private RecoredManager recoredManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        recoredManager = new RecoredManager();
        recoredManager.startRecord();


        /**抢占音频焦点**/
        AudioSetAndManager.getInstance(getApplicationContext()).pauseMusic();

        startCallTime();//开始计时

        LogUtil.showJohnLog(3, TAG + "-------CallingService---create----------");

        //发送广播给语音， //发送广播，正在拨号等待中
        sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_RINGING));

        mWindow = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);

        IntentFilter callIntent = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        callIntent.addAction(BtReceiverOtherOrder.BLUETOOTH_OFF);//收到蓝牙关闭的广播
        callIntent.addAction(BtReceiverOtherOrder.ACC_PARKING);//开启停车监控广播，关闭本界面
        registerReceiver(callReceiver, callIntent);

        EventBus.getDefault().register(this);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initWindow();
        initData();
        mWindow.addView(mInflateView, params);
        return super.onStartCommand(intent, flags, startId);
    }


    private void initWindow() {

        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_PHONE;
        params.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        params.format = PixelFormat.TRANSLUCENT;

        mInflateView = (BackLinearLayout) inflater.inflate(R.layout.service_calling, null);

        mIbCallSwitchVoice = (ImageButton) mInflateView.findViewById(R.id.call_ib_voice_switch);//切换声音
        mIbCallHungUp = (ImageButton) mInflateView.findViewById(R.id.call_ib_hung_up);//挂断电话
        mIbGoDials = (ImageButton) mInflateView.findViewById(R.id.call_ib_go_dials);//返回界面
        mTvCallName = (TextView) mInflateView.findViewById(R.id.call_tv_number);//显示电话或者名字

        //防止重复切换声音,设置点击的时间间隔
        mIbCallSwitchVoice.setOnClickListener(new NoDoubleClickListener(BtDefaultValue.DELAY_TIME) {
            @Override
            public void singleClick(View view) {
                BlueToothJniTool.getJniToolInstance(getApplicationContext()).sendEasyCommand(JniConfigOder.BT_SWITCH_VOICE);
            }
        });


        mIbCallHungUp.setOnClickListener(this);
        mIbGoDials.setOnClickListener(this);

        //拦截back键
        mInflateView.setmDispatchKeyEventListener(dispatchKeyEventListener);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.call_ib_hung_up:
                //挂断电话
                BlueToothJniTool.getJniToolInstance(this).sendEasyCommand(JniConfigOder.BT_HAND_UP);
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                }

                //手动添加，通话结束状态
                PreferenceUtils.putInt(getApplicationContext(), PrefrenceConstant.STORAGE_STATE, BtStates.BT_STATE_PHONE_TALK_END);
                stopSelf();
                break;
            case R.id.call_ib_go_dials://关闭界面
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                }
                break;
        }
    }


    private void initData() {
        mTvCallName.setText(PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.PHONE_SUDDENLY_CUT));
        //判断是前台还是后台运行
        boolean isBaiDu = AppUtil.getInstance(getApplicationContext()).isMapForground(getApplicationContext(), AppPackageName.BAIDUMAP_APP);
        boolean isGaoDe = AppUtil.getInstance(getApplicationContext()).isMapForground(getApplicationContext(), AppPackageName.GAODEMAP_APPLITE);
        LogUtil.showJohnLog(3, TAG + "-------isBaiDu--------" + isBaiDu + "--------------isGaoDe---------" + isGaoDe);
        if (isBaiDu || isGaoDe) {
            mInflateView.getBackground().setAlpha(100);
        } else {
            mInflateView.getBackground().setAlpha(255);
        }
        //回显
        String stringNumber = PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.CALLING_TELEPHONE_NUMBER, null);
        if (stringNumber != null) {
            mTvCallName.setText(stringNumber);
        }

    }

    /**
     * 拦截back键
     */
    BackLinearLayout.DispatchKeyEventListener dispatchKeyEventListener = new BackLinearLayout.DispatchKeyEventListener() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (mInflateView != null) {
                    mWindow.removeView(mInflateView);
                }
                return true;
            }
            return false;
        }
    };


    /**
     * 监听home键,以及跳转和挂断
     */
    BroadcastReceiver callReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.showJohnLog(3, TAG + "----------call receiver ----action--- " + action);
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(BtDefaultValue.SYSTEM_DIALOG_REASON_KEY);
                if (BtDefaultValue.SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {

                    if (mInflateView.getParent() != null) {
                        mWindow.removeView(mInflateView);
                    }
                }

            } else if (action.equals(BtReceiverOtherOrder.BLUETOOTH_OFF)) {//收到蓝牙关闭的广播
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                    //发送蓝牙断开的bus消息
                    EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_PHONE_TALK_END));
                    EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_CUT));
                }
                stopSelf();
            } else if (action.equals(BtReceiverOtherOrder.ACC_PARKING)) {//进入停车监控
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        LogUtil.showJohnLog(3, TAG + "----------CallingService--destroy----------");
        unregisterReceiver(callReceiver);

        //电话号码为空
        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.BLUE_TOOTH_TELEPHONE_CALLING_NUMBER, null);

        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.CALLING_TELEPHONE_NUMBER, null);

        //服务摧毁的时候关闭计时器
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (mInflateView.getParent() != null) {
            mWindow.removeView(mInflateView);
        }
        //正在拨打电话突然断电时候
        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.PHONE_SUDDENLY_CUT, "");
        super.onDestroy();
        recoredManager.cancelRecord();

        /**释放音频焦点**/
//        AudioSetAndManager.getInstance(getApplicationContext()).startMusic();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniNumber event) {
        getRcNumber(event.jniNumber);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {
        if (event.btState == BtStates.BT_STATE_PHONE_TALK_END) {//挂断的广播
            if (mInflateView.getParent() != null) {
                mWindow.removeView(mInflateView);
            }
            stopSelf();
        }
    }

    /**
     * 获取正在拨打的号码
     *
     * @param rcNumber
     */
    private void getRcNumber(String rcNumber) {
        String callingNumber = GetTelephoneNumber.getCallingNumber(rcNumber);
        //从数据库中查询电话号码的名称
        List<ContactBean> contactBeen = DataSupport.where("phoneNumber=?", callingNumber).find(ContactBean.class);
        if (contactBeen.size() == 0) {
            mTvCallName.setText(callingNumber);
        } else {
            ContactBean contactBean = contactBeen.get(0);
            mTvCallName.setText(contactBean.getName());
        }

//        PreferenceUtils.putString(getApplicationContext(), "TelephoneNumber", mTvCallName.getText().toString());
        //存起来，以备下次进入再用
        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.CALLING_TELEPHONE_NUMBER, mTvCallName.getText().toString());

        LogUtil.showJohnLog(3, TAG + "---takeNumber----" + rcNumber + "-----need----" + callingNumber);
    }


    //处理计时器返回的数据
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            LogUtil.showJohnLog(3, TAG + "-------handler的消息：" + msg.what);
            if (msg.what < 60 && PreferenceUtils.getInt(getApplicationContext(), PrefrenceConstant.STORAGE_STATE) == BtStates.BT_STATE_PHONE_TALKING) {
                //电话已经接通
                //关闭计时器
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }

                /**向语音发送接听广播*/
                sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_OFFHOOK));
                stopSelf();
            } else if (msg.what >= 60) {//大于60s且不在通话状态

                LogUtil.showJohnLog(3, TAG + "---------等待大于60s：" + msg.what);

                //关闭计时器
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                //停止服务
                /**向语音发送对方未接听广播*/
                sendBroadcast(new Intent(BtToAIOSCastOrder.SEND_AIOS_OUTGOING_IDLE));
                stopSelf();
            }
        }
    };

    /**
     * 计时器(拨打电话超过60秒就停止)
     */
    private void startCallTime() {
        timer = new Timer();
        TimerTask task = new TimerTask() {
            int i = 1;

            @Override
            public void run() {
                //定义一个消息传递
                Message message = new Message();
                message.what = i++;
                handler.sendMessage(message);
//                //号码如果为空,那就继续查询
//                if (PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.BLUE_TOOTH_TELEPHONE_CALLING_NUMBER) == null) {
//                    LogUtil.showJohnLog(3, TAG + "-----------继续查询号码------------");
//                    BlueToothJniTool.getJniToolInstance(getApplicationContext()).recentCalledNumber();
//                }
            }
        };
        timer.schedule(task, 500, 1500);//0秒后开始计时，计时的间隔为1s。
    }

}
