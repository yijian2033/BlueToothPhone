package com.conqueror.bluetoothphone.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.conqueror.bluetoothphone.R;

/**
 * 自定义的Dialog类
 */

public class DialogView {


    private final AlertDialog mDialog;

    public DialogView(Context context) {
        mDialog = new AlertDialog.Builder(context, R.style.AlertDialogCustom).create();
    }

    public void showDialog(String title, View view, DialogInterface.OnClickListener pListener) {
        mDialog.setTitle(title);
        mDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDialog.setView(view);
        mDialog.setCanceledOnTouchOutside(false);//点击dialog以外而不是去焦点
        mDialog.setCancelable(false);
        mDialog.setButton(Dialog.BUTTON_POSITIVE, "确定", pListener);
        mDialog.setButton(Dialog.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }

    public void dialogDismiss() {
        mDialog.dismiss();
    }
}
