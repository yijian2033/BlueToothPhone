package com.conqueror.bluetoothphone.manager;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import com.conqueror.bluetoothphone.util.LogUtil;
import com.conqueror.bluetoothphone.util.PreferenceUtils;

public class AudioSetAndManager {

    private static final String TAG = AudioSetAndManager.class.getName();

    private static AudioSetAndManager instance = null;
    private Context mContext;
    private final AudioManager mAm;
    private MyOnAudioFocusChangeListener mListener;

    private AudioSetAndManager(Context context) {
        this.mContext = context;
        mAm = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mListener = new MyOnAudioFocusChangeListener();

//        mAm.setStreamMute(AudioManager.STREAM_MUSIC, false);//取消静音

        //存储音量,设置声音
        int streamVolume = mAm.getStreamVolume(AudioManager.STREAM_MUSIC);
        LogUtil.showJohnLog(3, TAG + "---系统 voice 大小-------" + streamVolume);
        PreferenceUtils.putInt(mContext.getApplicationContext(), "Voice", streamVolume);
    }

    public static AudioSetAndManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AudioSetAndManager.class) {
                instance = new AudioSetAndManager(context);
            }
        }
        return instance;
    }

    public void pauseMusic() {

        mAm.setStreamVolume(AudioManager.STREAM_MUSIC, 6, AudioManager.FLAG_PLAY_SOUND);
        //抢占焦点
        int result = mAm.requestAudioFocus(mListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.i(TAG, "requestAudioFocus successfully.");
        } else {
            Log.e(TAG, "requestAudioFocus failed.");
        }

    }

    public void startMusic() {

        LogUtil.showJohnLog(3, TAG + "----get voice 大小-------" + PreferenceUtils.getInt(mContext.getApplicationContext(), "Voice", 10));

        int voice = PreferenceUtils.getInt(mContext.getApplicationContext(), "Voice", 10);
        if (voice == 0) {
            voice = 10;
        }

        //设置声音
        mAm.setStreamVolume(AudioManager.STREAM_MUSIC, voice, AudioManager.FLAG_PLAY_SOUND);
        //释放焦点
        mAm.abandonAudioFocus(mListener);

        mAm.setStreamMute(AudioManager.STREAM_MUSIC, false);//取消静音

        if (instance != null) {
            instance = null;
        }

    }

    private class MyOnAudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
        }
    }
}
