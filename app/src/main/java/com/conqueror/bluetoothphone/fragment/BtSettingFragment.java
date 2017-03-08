package com.conqueror.bluetoothphone.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.activity.MainActivity;
import com.conqueror.bluetoothphone.base.BaseFragment;
import com.conqueror.bluetoothphone.bean.ContactBean;
import com.conqueror.bluetoothphone.bus.BusJniBtState;
import com.conqueror.bluetoothphone.bus.BusNativeBtName;
import com.conqueror.bluetoothphone.bus.BusPhoneBtName;
import com.conqueror.bluetoothphone.constant.BtDefaultValue;
import com.conqueror.bluetoothphone.constant.BtReceiverOtherOrder;
import com.conqueror.bluetoothphone.constant.BtStates;
import com.conqueror.bluetoothphone.constant.JniConfigOder;
import com.conqueror.bluetoothphone.constant.PrefrenceConstant;
import com.conqueror.bluetoothphone.factory.ThreadPoolProxyFactory;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;
import com.conqueror.bluetoothphone.util.ToastLoadContactUtil;
import com.conqueror.bluetoothphone.view.DialogView;
import com.conqueror.bluetoothphone.view.MySettingImageButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

/**
 * 设置界面
 */

public class BtSettingFragment extends BaseFragment implements View.OnClickListener {

    private View setView;
    private Button mBtBackButton;
    private ImageButton mIbBackHome;
    private RadioButton mRbBlueToothName, mRbCutBlueTooth;
    private TextView mTvShowBtName, mTvShowLinkState;
    private MySettingImageButton mBtAutoLink;
    private MySettingImageButton mBtAutoPhone;
    private MySettingImageButton mCleanContactCache;
    private static final String TAG = BtSettingFragment.class.getName();

    @Override
    public View initView() {
        setView = View.inflate(getContext(), R.layout.fragment_setting, null);
        mBtBackButton = (Button) setView.findViewById(R.id.bt_set_backButton);//箭头返回
        mIbBackHome = (ImageButton) setView.findViewById(R.id.bt_set_ib_backHome);//home键返回
        mRbBlueToothName = (RadioButton) setView.findViewById(R.id.bt_set_rb_bluetoothName);//重设蓝牙名称
        mRbCutBlueTooth = (RadioButton) setView.findViewById(R.id.bt_set_rb_cutBluetooth);//断开蓝牙
        mTvShowBtName = (TextView) setView.findViewById(R.id.bt_set_tv_showBtName);//显示本机蓝牙名称
        mTvShowLinkState = (TextView) setView.findViewById(R.id.bt_set_tv_showLinkState); //显示连接蓝牙名称

        //自动连接
        mBtAutoLink = (MySettingImageButton) setView.findViewById(R.id.blue_set_autoLinkToggle);
        boolean isAutoLink = PreferenceUtils.getBoolean(getContext().getApplicationContext(), PrefrenceConstant.SET_AUTO_LINK_KEY, true);
        mBtAutoLink.setToggleState(isAutoLink);


        //自动接听电话
        mBtAutoPhone = (MySettingImageButton) setView.findViewById(R.id.blue_set_autoPhoneToggle);
        boolean isAutoAnswer = PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_AUTO_ANSWER_KEY, false);
        mBtAutoPhone.setToggleState(isAutoAnswer);


        //是否缓存联系人
        mCleanContactCache = (MySettingImageButton) setView.findViewById(R.id.blue_set_cleanContactCache);
        boolean isClean = PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_CLEAN_CACHE_KEY, true);
        mCleanContactCache.setToggleState(isClean);

        return setView;
    }

    @Override
    public void initListener() {
        mBtBackButton.setOnClickListener(this);
        mIbBackHome.setOnClickListener(this);
        if (PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.BT_POWER_STATE, true)) {
            mRbBlueToothName.setOnClickListener(this);//设置蓝牙名称
        }

        mRbCutBlueTooth.setOnClickListener(this);
        mBtAutoLink.setOnClickListener(this);//自动连接蓝牙
        mBtAutoPhone.setOnClickListener(this);//自动接听电话
        mCleanContactCache.setOnClickListener(this);//清空联系人缓存
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_set_backButton:
            case R.id.bt_set_ib_backHome:
                MainActivity activity = (MainActivity) getActivity();
                activity.showBtFragment(true, activity.getFragment(0));
                break;
            case R.id.bt_set_rb_bluetoothName://设置本机蓝牙名称
                setBtName();
                break;
            case R.id.bt_set_rb_cutBluetooth://断开蓝牙
                cutBT();
                BlueToothJniTool.getJniToolInstance(getContext()).sendEasyCommand(JniConfigOder.BT_FAST_MATCH);
                break;

            case R.id.blue_set_autoLinkToggle://自动连接蓝牙
                mBtAutoLink.toggle();
                PreferenceUtils.putBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_AUTO_LINK_KEY, mBtAutoLink.getToggleState());
                break;
            case R.id.blue_set_autoPhoneToggle://自动接听电话
                mBtAutoPhone.toggle();
                PreferenceUtils.putBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_AUTO_ANSWER_KEY, mBtAutoPhone.getToggleState());
                break;
            case R.id.blue_set_cleanContactCache://是否缓存联系人
                mCleanContactCache.toggle();
                PreferenceUtils.putBoolean(getActivity().getApplicationContext(), PrefrenceConstant.SET_CLEAN_CACHE_KEY, mCleanContactCache.getToggleState());
                break;

        }
    }

    /**
     * 断开蓝牙
     */
    private void cutBT() {
        if (PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_DEVICE_LINKED) {

            //如果正在下载电话本
            if (PreferenceUtils.getBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false)) {

                ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            LogUtil.showJohnLog(3, TAG + "----------isLoadingContact------cancelDownload------");
                            //取消下载电话
                            BlueToothJniTool.getJniToolInstance(getContext()).sendEasyCommand(JniConfigOder.BT_CANCEL_DOWNLOAD_PHONEBOOK);

                            /**重新存储联系人状态*/
                            PreferenceUtils.putBoolean(getActivity().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false);
                            Thread.sleep(1500);
                            //发送相同的命令，是链接就会断，断开就会链接
                            BlueToothJniTool.getJniToolInstance(getContext()).sendEasyCommand(JniConfigOder.BT_FAST_MATCH);
                            while (true) {
                                SystemClock.sleep(1000);
                                int count = DataSupport.count(ContactBean.class);
                                LogUtil.showJohnLog(3, TAG + "-----while  find the count -----------" + count);
                                if (count > 0) {
                                    DataSupport.deleteAll(ContactBean.class);
                                    break;
                                } else {
                                    break;
                                }
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                    }
                });

            }
        } else {

            ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                        //发送相同的命令，是链接就会断，断开就会链接
                        BlueToothJniTool.getJniToolInstance(getContext()).sendEasyCommand(JniConfigOder.BT_FAST_MATCH);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            mTvShowBtName.setText(PreferenceUtils.getString(getContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, BtDefaultValue.BT_DEFAULT_NAME + "_000"));
            //显示连接的蓝牙名称
            mTvShowLinkState.setText(PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, 0)
                    == BtStates.BT_STATE_DEVICE_LINKED
//                    || PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, 0) == BtStates.BT_STATE_LINKED
                    ?
                    PreferenceUtils.getString(getContext(), PrefrenceConstant.BT_DEVICE_NAME, BtDefaultValue.DEFAULT_LINK_PHONE_BT_NAME) : "未连接");
            //断开蓝牙显示：
            mRbCutBlueTooth.setCompoundDrawablesWithIntrinsicBounds(0,
                    PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_DEVICE_LINKED
//                            || PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, 0) == BtStates.BT_STATE_LINKED
                            ?
                            R.mipmap.blue_setui_bluetooth_link : R.mipmap.blue_setui_bluetooth_break, 0, 0);


            if (DataSupport.findAll(ContactBean.class).size() == 0
                    && PreferenceUtils.getInt(getActivity().getApplicationContext(), PrefrenceConstant.BT_LINK_CUT_STATE) == BtStates.BT_STATE_DEVICE_LINKED
                    && (!PreferenceUtils.getBoolean(getContext().getApplicationContext(), PrefrenceConstant.CONTACT_LOADING_STATE, false))) {
                ToastLoadContactUtil.getInstance().showToastContact(getActivity());
            }


        }
    }

    @Override
    public void initData() {
        getActivity().registerReceiver(receiver, new IntentFilter(BtReceiverOtherOrder.ENTER_PARKING));
    }


    /**
     * 设置蓝牙名称
     */

    private void setBtName() {
        final EditText editText = new EditText(getActivity());
        editText.setTextColor(Color.WHITE);
        final DialogView dialogView = new DialogView(getActivity());
        dialogView.showDialog("请输入蓝牙名称", editText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(editText.getText().toString().trim())) {
                    Toast.makeText(getContext(), "修改名称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(getContext(), "修改名称" + editText.getText().toString(), Toast.LENGTH_SHORT).show();
                    mTvShowBtName.setText(editText.getText().toString());
                    BlueToothJniTool.getJniToolInstance(getContext()).setBluetoothName(editText.getText().toString());
                    //存储蓝牙名称
                    PreferenceUtils.putString(getContext(), PrefrenceConstant.SET_NATIVE_BT_NAME, editText.getText().toString());
                    dialogView.dialogDismiss();

                }
            }
        });
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(receiver);
    }

    /**
     * 蓝牙状态
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BusJniBtState event) {
        LogUtil.showJohnLog(3, TAG + "----state-----" + event.btState);
        if (event.btState == BtStates.BT_STATE_DEVICE_LINKED
//                || event.btState == BtStates.BT_STATE_LINKED
                ) {
//            mTvShowLinkState.setText(PreferenceUtils.getString(getContext(), PrefrenceConstant.BT_DEVICE_NAME, "已连接"));
            //L连接蓝牙显示：
            mRbCutBlueTooth.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.blue_setui_bluetooth_link, 0, 0);
        } else if (event.btState == BtStates.BT_STATE_CUT) {
            mTvShowLinkState.setText("未连接");
            //断开蓝牙显示：
            mRbCutBlueTooth.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.blue_setui_bluetooth_break, 0, 0);
        }
    }

    /**
     * 设置本地的蓝牙名称
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageLinkEvent(BusNativeBtName event) {

        LogUtil.showJohnLog(3, TAG + "---------收到本地蓝牙名称----" + event.nativeBtName);
        if (event.nativeBtName != null) {
            mTvShowBtName.setText(event.nativeBtName);
            BlueToothJniTool.getJniToolInstance(getContext()).setBluetoothName(event.nativeBtName);
        }

    }

    /**
     * 连接的蓝牙名称
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageLinkEvent(BusPhoneBtName event) {

        if (BtStates.BT_STATE_DEVICE_LINKED ==
                PreferenceUtils.getInt(getContext(), PrefrenceConstant.BT_LINK_CUT_STATE, BtStates.BT_STATE_CUT)) {
            mTvShowLinkState.setText(event.phoneName);
        }

    }

    @Override
    public boolean onBackPressed() {
        MainActivity activity = (MainActivity) getActivity();
        activity.showBtFragment(true, activity.getFragment(0));
        return true;
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTvShowLinkState.setText("未连接");
            //断开蓝牙显示：
            mRbCutBlueTooth.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.blue_setui_bluetooth_break, 0, 0);
        }
    };
}
