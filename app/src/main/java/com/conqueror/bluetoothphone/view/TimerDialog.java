package com.conqueror.bluetoothphone.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;
import android.widget.Button;

import com.conqueror.bluetoothphone.R;
import com.conqueror.bluetoothphone.util.LogUtil;

/**
 * Created by Administrator on 2016/10/28.
 */

public class TimerDialog {

    private static final int TYPE_POSITIVE = 1;
    private static final int TYPE_NEGATIVE = 2;
    private static final String TAG = TimerDialog.class.getName();

    private Context mContext;
    private Button p = null;
    private Button n = null;
    private int mPositiveCount = 0;
    private int mNegativeCount = 0;
    private AlertDialog mDialog = null;

    public TimerDialog(Context ctx) {
        mContext = ctx;

        mDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
    }

    public void setMessage(String msg) {
        mDialog.setMessage(msg);
    }

    public void setTitle(int resId) {
        mDialog.setTitle(resId);
    }

    public void setTitle(String title) {
        mDialog.setTitle(title);
    }

    public void show() {
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND, 0);

        mDialog.setCanceledOnTouchOutside(false);//点击dialog以外而不是去焦点
        mDialog.setCancelable(false);
        mDialog.show();

    }

    public void setPositiveButton(String text, DialogInterface.OnClickListener listener, int count) {
        text = getTimeText(text, count);
        mDialog.setButton(Dialog.BUTTON_POSITIVE, text, listener);
    }


    public void setNegativeButton(String text, DialogInterface.OnClickListener listener, int count) {
        text = getTimeText(text, count);
        mDialog.setButton(Dialog.BUTTON_NEGATIVE, text, listener);
    }

    public void cancelDialog() {
        mDialog.dismiss();
        mDialog.cancel();
        if (mHandler != null) {
            mHandler.removeMessages(TYPE_POSITIVE);
            mHandler.removeMessages(TYPE_NEGATIVE);
        }
    }

    /**
     * @param type      类型
     *                  Dialog.BUTTON_POSITIVE
     *                  Dialog.BUTTON_NEGATIVE
     * @param count     以s为单位
     * @param isDisable 是否可点击
     */
    public void setButtonType(int type, int count, boolean isDisable) {

        if (count <= 0) {
            return;
        }

        if (type == Dialog.BUTTON_POSITIVE) {
            p = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            p.setEnabled(isDisable);
            mPositiveCount = count;
            mHandler.sendEmptyMessageDelayed(TYPE_POSITIVE, 200);
        } else {
            if (type == Dialog.BUTTON_NEGATIVE) {
                n = mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                n.setEnabled(isDisable);
                mNegativeCount = count;
                mHandler.sendEmptyMessageDelayed(TYPE_NEGATIVE, 200);
            }
        }
    }


    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) {
                case TYPE_NEGATIVE:
                    if (mNegativeCount > 0) {
                        mNegativeCount--;
                        LogUtil.showJohnLog(3, "----mNegativeCount-----" + mNegativeCount);
                        if (n != null) {
                            String text = (String) n.getText();
                            n.setText(getTimeText(text, mNegativeCount));
                        }
                        mHandler.sendEmptyMessageDelayed(TYPE_NEGATIVE, 1000);
                    } else {

                        if (n != null) {
                            if (n.isEnabled()) {
                                n.performClick();
                            } else {
                                n.setEnabled(true);
                            }
                        }
                    }
                    break;
                case TYPE_POSITIVE:
                    if (mPositiveCount > 0) {
                        mPositiveCount--;
                        LogUtil.showJohnLog(3, TAG + "------mPositiveCount----" + mPositiveCount);
                        if (p != null) {
                            String text = (String) p.getText();
                            p.setText(getTimeText(text, mPositiveCount));
                        }
                        mHandler.sendEmptyMessageDelayed(TYPE_POSITIVE, 1000);
                    } else {

                        if (p != null) {
                            if (p.isEnabled()) {
                                p.performClick();
                            } else {
                                p.setEnabled(true);
                            }
                        }
                    }
                    break;
            }
        }
    };


    private String getTimeText(String text, int count) {
        if (text != null && text.length() > 0 && count > 0) {
            int index = text.indexOf("(");
            if (index > 0) {
                text = text.substring(0, index);
                return (text + "(" + count + "s)");
            } else {
                return (text + "(" + count + "s)");
            }

        }
        return text;
    }

}