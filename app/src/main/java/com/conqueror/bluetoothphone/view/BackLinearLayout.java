package com.conqueror.bluetoothphone.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.LinearLayout;

/**
 * 在服务中用来拦截back键
 */

public class BackLinearLayout extends LinearLayout {

    private DispatchKeyEventListener mDispatchKeyEventListener;

    public BackLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackLinearLayout(Context context) {
        this(context, null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {

        if (mDispatchKeyEventListener != null) {
            return mDispatchKeyEventListener.dispatchKeyEvent(event);
        }

        return super.dispatchKeyEvent(event);
    }

    public void setmDispatchKeyEventListener(DispatchKeyEventListener dispatchKeyEventListener) {
        this.mDispatchKeyEventListener = dispatchKeyEventListener;
    }

    public DispatchKeyEventListener getmDispatchKeyEventListener() {
        return mDispatchKeyEventListener;
    }

    //借口回调
   public interface DispatchKeyEventListener {
        boolean dispatchKeyEvent(KeyEvent event);
    }
}
