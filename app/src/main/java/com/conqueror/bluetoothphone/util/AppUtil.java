package com.conqueror.bluetoothphone.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.conqueror.bluetoothphone.R;

import java.util.List;

/**
 * App通用工具类
 */

public class AppUtil {

    private static final String TAG = AppUtil.class.getName();

    private static AppUtil instance;

    private Context mContext;

    public static AppUtil getInstance(Context context) {
        if (instance == null) {
            return instance = new AppUtil(context);
        }
        return instance;
    }

    private AppUtil(Context context) {
        this.mContext = context;
    }

    /**
     * 打开第三方应用
     *
     * @param packgeName
     * @param activityName
     */
    public void openApplication(String packgeName, String activityName) {
        ComponentName componentName = new ComponentName(packgeName, activityName);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(componentName);
        try {
            mContext.getApplicationContext().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(mContext.getApplicationContext(), R.string.app_not_install, Toast.LENGTH_SHORT).show();
            LogUtil.showJohnLog(3, TAG + "------" + e.toString());
        }
    }

    /**
     * Toast方法
     *
     * @param context
     * @param str
     */
    public void myToast(Context context, String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }


    /**
     * 判断app是否在前台运行
     *
     * @param context
     * @param packageName
     * @return
     */
    public boolean isMapForground(Context context, String packageName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
        ActivityManager.RunningAppProcessInfo appTop = appProcesses.get(0);
        LogUtil.showJohnLog(3, TAG + "--------TopApp------" + appTop.processName);
        if (appTop != null && appTop.processName.equals(packageName)) {
            return true;
        }
        return false;
    }

}
