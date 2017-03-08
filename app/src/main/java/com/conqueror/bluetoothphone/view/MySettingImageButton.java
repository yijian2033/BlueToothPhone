package com.conqueror.bluetoothphone.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.conqueror.bluetoothphone.R;

/**
 * 设置界面的开关ImageButton
 */
public class MySettingImageButton extends LinearLayout {

    private TextView mTvToggle;
    private ImageView mAutoToggle;
    private boolean isOpen = true;

    public MySettingImageButton(Context context) {
        this(context, null);
    }

    //布局的时候使用
    public MySettingImageButton(Context context, AttributeSet set) {
        super(context, set);
        //挂载xml布局，绑定当前类
        View.inflate(context, R.layout.blue_set_toggle, this);

        mAutoToggle = (ImageView) findViewById(R.id.blue_set_iv_autotoggle);
        mTvToggle = (TextView) findViewById(R.id.blue_set_tv_toggle);

        //读取自定义属性
        TypedArray ta = context.obtainStyledAttributes(set, R.styleable.BlueSettingToggleView);

        //读属性
        String title = ta.getString(R.styleable.BlueSettingToggleView_tibTitle);

        //回收自定义属性
        ta.recycle();

        //设置开关描述
        mTvToggle.setText(title);

    }

    /**
     * 开关的方法:开的时候就关，关的时候就开
     */
    public void toggle() {

        mAutoToggle.setBackgroundResource(isOpen ? R.drawable.blue_setui_off_selector : R.drawable.blue_setui_turnon_selector);
        // 重置状态
        isOpen = !isOpen;
    }

    /**
     * 获得开关的状态
     *
     * @return
     */
    public boolean getToggleState() {
        return isOpen;
    }

    /**
     * 设置开关的状态
     *
     * @param state
     */
    public void setToggleState(boolean state) {
        this.isOpen = state;
        mAutoToggle.setBackgroundResource(isOpen ? R.drawable.blue_setui_turnon_selector : R.drawable.blue_setui_off_selector);
    }
}
