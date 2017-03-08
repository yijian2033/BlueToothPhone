package com.conqueror.bluetoothphone.util;

import android.content.Context;
import android.content.Intent;

import com.conqueror.bluetoothphone.constant.BtToAIOSCastOrder;

/**
 * Created by Administrator on 2017/2/9.
 */

public class AIOSTTSpeakUtil {


    private static AIOSTTSpeakUtil instance;

    private Context context;

    public AIOSTTSpeakUtil(Context context) {
        this.context = context;
    }

    public static AIOSTTSpeakUtil getInstance(Context context) {
        if (instance == null) {
            return new AIOSTTSpeakUtil(context);
        }
        return instance;
    }

    /**
     * 发送TTS语音播报
     *
     * @param content
     */
    public void sendTTSpeak(String content) {
        Intent tts = new Intent(BtToAIOSCastOrder.AIOS_TTS_SPEAK);
        tts.putExtra("text", content);
        context.sendBroadcast(tts);
    }
}
