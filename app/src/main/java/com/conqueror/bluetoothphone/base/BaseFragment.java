package com.conqueror.bluetoothphone.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.conqueror.bluetoothphone.handlerBack.FragmentBackHandler;
import com.conqueror.bluetoothphone.util.LogUtil;

import java.lang.reflect.Field;

/**
 * Fragment的公共基础类
 */

public abstract class BaseFragment extends Fragment implements FragmentBackHandler {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return initView();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();
        initListener();
    }


    /**
     * 监听事件
     */
    public void initListener() {

    }

    /**
     * 数据加载
     */
    public void initData() {

    }

    /**
     * View的加载，子类必须实现
     *
     * @return view
     */
    public abstract View initView();

    /**
     * 一开始就需要加载的数据
     */
    public void init() {

    }


    @Override
    public boolean onBackPressed() {
        return isBackPressed();
    }


    public boolean isBackPressed() {
        return false;
    }
}
