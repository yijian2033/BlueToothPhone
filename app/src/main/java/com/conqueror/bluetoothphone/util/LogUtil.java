package com.conqueror.bluetoothphone.util;

import android.util.Log;

/**
 * 自定义的log
 */

public class LogUtil {
    private static final String TAG = "JohnLog";
    private static final String SYMBOL = "---------";

    public static void showJohnLog(int i, String log) {
        switch (i) {
            case 0:
                Log.d(TAG, SYMBOL + log);
                break;
            case 1:
                Log.i(TAG, SYMBOL + log);
                break;
            case 2:
                Log.w(TAG, SYMBOL + log);
                break;
            case 3:
                Log.e(TAG, SYMBOL + log);
                break;
        }
    }


}
