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
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.constant.AppPackageName;
import com.conqueror.bluetoothphone.constant.BtDefaultValue;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.AudioSetAndManager;
import com.conqueror.bluetoothphone.manager.RecoredManager;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.LogUtil;

import com.conqueror.bluetoothphone.view.BackLinearLayout;
import com.conqueror.bluetoothphone.view.NoDoubleClickListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;


/**
 * 初步判断响铃不需要取关，只需要一些命令即可，可在响铃服务中警醒一些操作
 */
public class TalkingService extends Service implements View.OnClickListener {

    private WindowManager mWindow;
    private LayoutInflater inflater;
    private BackLinearLayout mInflateView;
    private ImageButton mIbSwitchVoice;
    private ImageButton mIbHungUp;
    private TextView mTvShowTalkTime;
    private WindowManager.LayoutParams params;
    private Timer timer;

    private static final String TAG = TalkingService.class.getName();

    private long startTalkTime = System.currentTimeMillis();//获取系统当前的时间
    private RecoredManager recoredManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        EventBus.getDefault().register(this);

        recoredManager = new RecoredManager();
        recoredManager.startRecord();

        /**抢占音频焦点**/
        AudioSetAndManager.getInstance(getApplicationContext()).pauseMusic();
        super.onCreate();
        LogUtil.showJohnLog(3, TAG + "------talk service create-------");

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//        intentFilter.addAction(SendBroadCastOrders.TALK_END);//通话结束广播
        intentFilter.addAction(BtReceiverOtherOrder.BLUETOOTH_OFF);//关闭蓝牙的广播
        intentFilter.addAction(BtReceiverOtherOrder.ACC_PARKING);//进入停车监控
        registerReceiver(talkReceiver, intentFilter);

        mWindow = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        inflater = LayoutInflater.from(this);
        timer = new Timer();
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

        mInflateView = (BackLinearLayout) inflater.inflate(R.layout.service_talking, null);

        mIbSwitchVoice = (ImageButton) mInflateView.findViewById(R.id.call_ib__talking_voice_switch);
        mIbHungUp = (ImageButton) mInflateView.findViewById(R.id.call_ib_talking_hung_up);
        mTvShowTalkTime = (TextView) mInflateView.findViewById(R.id.call_tv_talking_time);

//        mIbSwitchVoice.setOnClickListener(this);
        //为了避免重复点击，设置两次点击的时间间隔
        mIbSwitchVoice.setOnClickListener(new NoDoubleClickListener(BtDefaultValue.DELAY_TIME) {
            @Override
            public void singleClick(View view) {
                LogUtil.showJohnLog(3, TAG + "----------talking server click switch voice ------");
                BlueToothJniTool.getJniToolInstance(getApplicationContext()).sendEasyCommand(JniConfigOder.BT_SWITCH_VOICE);
            }
        });

        mIbHungUp.setOnClickListener(this);

        //拦截back键
        mInflateView.setmDispatchKeyEventListener(dispatchKeyEventListener);

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.call_ib_talking_hung_up://结束挂断
                BlueToothJniTool.getJniToolInstance(this).sendEasyCommand(JniConfigOder.BT_HAND_UP);
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                }
                //发送挂断广播
                EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_PHONE_TALK_END));
                stopSelf();
                break;
        }
    }

    /**
     * 拦截back键
     */
    BackLinearLayout.DispatchKeyEventListener dispatchKeyEventListener = new BackLinearLayout.DispatchKeyEventListener() {
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {

            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
                }
                return true;
            }

            return false;
        }
    };


    /**
     * 监听home键和其他的挂断广播
     */
    BroadcastReceiver talkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.showJohnLog(3, TAG + "-----taking service action -----" + action);
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(BtDefaultValue.SYSTEM_DIALOG_REASON_KEY);
                if (BtDefaultValue.SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {

                    if (mInflateView.getParent() != null) {
                        mWindow.removeView(mInflateView);
                    }
                }
//                else {
//                    if (mInflateView.getParent() != null) {
//                        mWindow.removeView(mInflateView);
//                    }
//                    stopSelf();
//                }
            } else if (action.equals(BtReceiverOtherOrder.BLUETOOTH_OFF)) {//关闭蓝牙
                if (mInflateView.getParent() != null) {
                    mWindow.removeView(mInflateView);
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
        if (mInflateView.getParent() != null) {
            mWindow.removeView(mInflateView);
        }
        unregisterReceiver(talkReceiver);

        if (timer != null) {
            timer.cancel();
        }
        LogUtil.showJohnLog(3, TAG + "------talk service destroy-------");
        super.onDestroy();
        recoredManager.cancelRecord();
        /**释放音频焦点**/
        AudioSetAndManager.getInstance(getApplicationContext()).startMusic();

    }

    private void initData() {

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                long nowTime = System.currentTimeMillis();
                long wantTime = nowTime - startTalkTime - 28800000;
                String showTime = sdf.format(wantTime);

                Message msg = new Message();
                msg.what = 1905;
                msg.obj = showTime;
                mHandler.sendMessage(msg);

                LogUtil.showJohnLog(3, TAG + "---------计时所用的时间差：" + wantTime + "-----展示各式：" + showTime);
            }
        }, 0, 1000);


        //判断是前台还是后台运行
        boolean isBaiDu = AppUtil.getInstance(getApplicationContext()).isMapForground(getApplicationContext(), AppPackageName.BAIDUMAP_APP);
        boolean isGaoDe = AppUtil.getInstance(getApplicationContext()).isMapForground(getApplicationContext(), AppPackageName.GAODEMAP_APPLITE);
        LogUtil.showJohnLog(3, TAG + "-------isBaiDu--------" + isBaiDu + "--------------isGaoDe---------" + isGaoDe);
        if (isBaiDu || isGaoDe) {
            mInflateView.getBackground().setAlpha(100);
        } else {
            mInflateView.getBackground().setAlpha(255);
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (1905 == msg.what) {
                String talkTime = (String) msg.obj;
                mTvShowTalkTime.setText(talkTime);
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {
        if (event.btState == BtStates.BT_STATE_PHONE_TALK_END) {//挂断的广播或者同通话
            if (mInflateView.getParent() != null) {
                mWindow.removeView(mInflateView);
            }
            stopSelf();
        }
    }

}
