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
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.bean.ContactBean;

import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusJniNumber;
import com.conqueror.bluetoothphone.constant.AppPackageName;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.manager.AudioSetAndManager;
import com.conqueror.bluetoothphone.util.AppUtil;
import com.conqueror.bluetoothphone.util.GetTelephoneNumber;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

import java.util.List;

public class RingService extends Service implements View.OnClickListener {

    private WindowManager mWManager;
    private LayoutInflater inflater;
    private View mInflateView;
    private WindowManager.LayoutParams layoutParams;
    private ImageView mIvAnserCall;
    private ImageView mIvRefuseCall;
    private TextView mTvCallName;

    private String aiosName = "";
    private String aiosNumber = "";

    private static final String TAG = RingService.class.getName();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        //注册bus
        EventBus.getDefault().register(this);
        inflater = LayoutInflater.from(this);

        /**抢占音频焦点**/
        AudioSetAndManager.getInstance(getApplicationContext()).pauseMusic();

        LogUtil.showJohnLog(3, TAG + "--------ring service start--");

        mWManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BtReceiverOtherOrder.BLUETOOTH_OFF);//蓝牙关闭的广播
        intentFilter.addAction(BtReceiverOtherOrder.ACC_PARKING);//停车监控
        registerReceiver(homeKeyReceiver, intentFilter);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initWindow();
        initData();
        mWManager.addView(mInflateView, layoutParams);
        return super.onStartCommand(intent, flags, startId);
    }


    private void initData() {
        //存储起来作为回显
        String ringNumber = PreferenceUtils.getString(getApplicationContext(), PrefrenceConstant.RING_TELEPHONE_NUMBER, null);

        if (ringNumber != null) {
            mTvCallName.setText(ringNumber);
        }

    }

    private void getRingNumber(String number) {

        final String ringingNumber = GetTelephoneNumber.getRingingNumber(number);

        //从数据库中查询电话号码的名称
        final List<ContactBean> contactBeen = DataSupport.where("phoneNumber=?", ringingNumber).find(ContactBean.class);

        if (contactBeen.size() == 0) {
            aiosName = ringingNumber;
            mTvCallName.setText(ringingNumber);
            //存储起来作为回显
            PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.RING_TELEPHONE_NUMBER, ringingNumber);
        } else {
            ContactBean contactBean = contactBeen.get(0);
            aiosName = contactBean.getName();
            mTvCallName.setText(aiosName);

            //存储起来作为回显
            PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.RING_TELEPHONE_NUMBER, aiosName);
        }
        LogUtil.showJohnLog(3, TAG + "---takeRing----" + number + "=====ringNeed====" + ringingNumber);

        aiosNumber = ringingNumber;

        /**向语音发送广播有来电*/
        Intent intent = new Intent(BtToAIOSCastOrder.SEND_AIOS_INCOMING_RINGING);
        intent.putExtra(BtToAIOSCastOrder.AIOS_NAME_KEY, aiosName);
        intent.putExtra(BtToAIOSCastOrder.AIOS_NUMBER_KEY, aiosNumber);
        sendBroadcast(intent);
    }


    private void initWindow() {

        //View部分
        mInflateView = inflater.inflate(R.layout.service_ring, null);
        mIvAnserCall = (ImageView) mInflateView.findViewById(R.id.ring_iv_answer_call);
        mIvRefuseCall = (ImageView) mInflateView.findViewById(R.id.ring_iv_refuse_call);
        mTvCallName = (TextView) mInflateView.findViewById(R.id.ring_tv_call_name);

        layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;//屏蔽所有的按键，包括back和home键
        layoutParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;//设置全屏
        layoutParams.format = PixelFormat.TRANSLUCENT;//支持透明

        mIvAnserCall.setOnClickListener(this);
        mIvRefuseCall.setOnClickListener(this);


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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ring_iv_answer_call:
                BlueToothJniTool.getJniToolInstance(this).sendEasyCommand(JniConfigOder.BT_ANSWER);
                if (mInflateView != null) {
                    mWManager.removeView(mInflateView);
                }
                stopSelf();
                break;
            case R.id.ring_iv_refuse_call:
                BlueToothJniTool.getJniToolInstance(this).sendEasyCommand(JniConfigOder.BT_REJECT);
                if (mInflateView != null) {
                    mWManager.removeView(mInflateView);
                }
                //为了以防万一发送一个电话断开的状态
                EventBus.getDefault().post(new BusJniBtState(BtStates.BT_STATE_PHONE_TALK_END));
                stopSelf();
                break;
        }
    }

    //监听home键
    BroadcastReceiver homeKeyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.showJohnLog(3, TAG + "------ring service get broadcast-----action-----" + action);
            if (action.equals(BtReceiverOtherOrder.ACC_PARKING)) {
                if (mInflateView.getParent() != null) {
                    mWManager.removeView(mInflateView);
                }
            }

        }
    };

    @Override
    public void onDestroy() {
        //取消注册bus
        EventBus.getDefault().unregister(this);

        //电话号码为空
        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.BLUE_TOOTH_TELEPHONE_CALLING_NUMBER, null);

        //存储起来作为回显
        PreferenceUtils.putString(getApplicationContext(), PrefrenceConstant.RING_TELEPHONE_NUMBER, null);

        super.onDestroy();
        LogUtil.showJohnLog(3, TAG + "--------ring service destroy--");
        unregisterReceiver(homeKeyReceiver);
        if (mInflateView.getParent() != null) {
            mWManager.removeView(mInflateView);
        }
        /**释放音频焦点**/
//        AudioSetAndManager.getInstance(getApplicationContext()).startMusic();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {
        if (event.btState == BtStates.BT_STATE_PHONE_TALK_END || event.btState == BtStates.BT_STATE_PHONE_TALKING) {//挂断的广播或者同通话
            if (mInflateView.getParent() != null) {
                mWManager.removeView(mInflateView);
            }
            stopSelf();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniNumber event) {
        if (event.jniNumber == null) {
            return;
        }
        getRingNumber(event.jniNumber);
    }

}
