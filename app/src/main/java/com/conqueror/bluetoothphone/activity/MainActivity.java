package com.conqueror.bluetoothphone.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.base.BaseFragment;
import com.conqueror.bluetoothphone.constant.BtReceiverAiosOrder;
import com.conqueror.bluetoothphone.fragment.BtMainFragment;
import com.conqueror.bluetoothphone.fragment.BtPhoneBookFragment;
import com.conqueror.bluetoothphone.fragment.BtRecentBookFragment;
import com.conqueror.bluetoothphone.fragment.BtSettingFragment;
import com.conqueror.bluetoothphone.handlerBack.BackHandlerHelper;
import com.conqueror.bluetoothphone.jni.BlueToothJniTool;
import com.conqueror.bluetoothphone.service.BlueToothService;

import org.litepal.tablemanager.Connector;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    //添加的图片
    public int[] images = new int[]{R.mipmap.phonebook, R.mipmap.one, R.mipmap.two,
            R.mipmap.three,
            R.mipmap.jing, R.mipmap.bluetooth_phone, R.mipmap.recent_calls, R.mipmap.four,
            R.mipmap.five, R.mipmap.six, R.mipmap.zero, R.mipmap.translation_phone,
            R.mipmap.robot, R.mipmap.seven, R.mipmap.eight, R.mipmap.nine,
            R.mipmap.xing, R.mipmap.bluetooth_setting
    };
    //添加的背景
    public int[] bgs = new int[]{R.drawable.blue_menu_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.blue_menu_selector,
            R.drawable.blue_menu_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.blue_menu_selector,
            R.drawable.blue_menu_left_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.keyboard_selector,
            R.drawable.keyboard_selector, R.drawable.blue_menu_right_selector
    };


    private static final String TAG = MainActivity.class.getName();
    private FragmentManager fragmentManager;
    private BtPhoneBookFragment btPhoneBookFragment;
    private BtRecentBookFragment btRecentBookFragment;
    private BtSettingFragment btSettingFragment;
    private BtMainFragment btMainFragment;
    private ArrayList<BaseFragment> fragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initJni();
        initFragment();

        registerReceiver(aiosReceiver, new IntentFilter(BtReceiverAiosOrder.AIOS_CLOSE_ACTIVITY));

    }

    private void initJni() {
        startService(new Intent(this, BlueToothService.class));

        //初始化数据库
//        Connector.getDatabase();
    }

    /**
     * 初始化fragment
     */
    private void initFragment() {
        //电话本
        btPhoneBookFragment = new BtPhoneBookFragment();

        //最近联系人
        btRecentBookFragment = new BtRecentBookFragment();
        //设置界面
        btSettingFragment = new BtSettingFragment();
        //主界面
        btMainFragment = new BtMainFragment();
        fragments = new ArrayList<>();
        fragments.add(btMainFragment);
        fragments.add(btPhoneBookFragment);
        fragments.add(btRecentBookFragment);
        fragments.add(btSettingFragment);

        fragmentManager = getSupportFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.activity_main_fl, btMainFragment, BtMainFragment.class.getName()).show(btMainFragment)
                .add(R.id.activity_main_fl, btPhoneBookFragment, BtPhoneBookFragment.class.getName()).hide(btPhoneBookFragment)
                .add(R.id.activity_main_fl, btRecentBookFragment, BtRecentBookFragment.class.getName()).hide(btRecentBookFragment)
                .add(R.id.activity_main_fl, btSettingFragment, BtSettingFragment.class.getName()).hide(btSettingFragment);
        transaction.commit();
    }

    public BaseFragment getFragment(int position) {
        for (int i = 0; i < fragments.size(); i++) {
            if (position == i) {
                return fragments.get(i);
            }
        }
        return null;
    }

    /**
     * 显示或者隐藏fragment
     *
     * @param isShow
     * @param btFragment
     */
    public void showBtFragment(boolean isShow, BaseFragment btFragment) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        for (BaseFragment fragment : fragments) {
            fragmentTransaction.hide(fragment);
        }
        if (isShow) {
            fragmentTransaction.show(btFragment);
        }

        fragmentTransaction.commit();
    }


    @Override
    public void onBackPressed() {
        //fragment没有处理back事件交给Activity处理
        if (!BackHandlerHelper.handleBackPress(this)) {
            super.onBackPressed();
        }
    }


    /**
     * 收到语音关闭广播
     */
    BroadcastReceiver aiosReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(aiosReceiver);
    }
}






