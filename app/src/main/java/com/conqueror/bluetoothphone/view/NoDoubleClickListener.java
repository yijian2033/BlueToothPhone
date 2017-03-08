package com.conqueror.bluetoothphone.view;

import android.view.View;

import com.conqueror.bluetoothphone.util.LogUtil;


/**
 * 为了解决重复点击的问题
 */

public abstract class NoDoubleClickListener implements View.OnClickListener {


    private static final String TAG = NoDoubleClickListener.class.getName();

    private static long lastTime;

    private long delay;

    public abstract void singleClick(View view);

    public NoDoubleClickListener(long delay) {
        this.delay = delay;
    }

    @Override
    public void onClick(View v) {
        if (onMoreClick(v)) {
            LogUtil.showJohnLog(3, TAG + "------------service voice click fast !!!!!!!!");
            return;
        }
        singleClick(v);
    }

    public boolean onMoreClick(View view) {
        boolean flag = false;
        long time = System.currentTimeMillis() - lastTime;
        if (time < delay) {
            flag = true;
        }
        lastTime = System.currentTimeMillis();
        return flag;
    }


}
