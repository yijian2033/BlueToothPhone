package com.conqueror.bluetoothphone.constant;

/**
 * Created by Administrator on 2016/5/6 0006.
 * 一些简易指令的值
 */
public interface JniConfigOder {


    /**
     * 命令的返回值 1 表示成功
     */
    int BT_STATE_SUCCESS = 1;//成功

    /**
     * 命令的返回值 0 表示失败
     */
    int BT_STATE_FAILE = 0;//失败


    /**
     * 快速配对
     */
    int BT_FAST_MATCH = 0;

    /**
     * 播放或者暂停（音乐）
     */
    int BT_STAR_OR_STOP = 1;

    /**
     * 下一曲
     */
    int BT_NEXT_SONG = 2;

    /**
     * 上一曲
     */
    int BT_PREVIOUS_SONG = 3;

    /**
     * 加声音
     */
    int BT_VOL_PLUS = 4;

    /**
     * 减声音
     */
    int BT_VOL_MINUS = 5;

    /**
     * 接听电话
     */
    int BT_ANSWER = 6;

    /**
     * 拒接电话
     */
    int BT_REJECT = 7;

    /**
     * 挂断电话
     */
    int BT_HAND_UP = 8;


    /**
     * 查询拨出的电话号码
     */
    int BT_QUERY_OUT_TELEPHONE_NUMBER = 9;

    /**
     * 下载电话本
     */
    int BT_DOWNLOAD_PHONEBOOK = 10;

    /**
     * 取消下载电话薄
     */
    int BT_CANCEL_DOWNLOAD_PHONEBOOK = 11;


    /**
     * 切换声音
     */
    int BT_SWITCH_VOICE = 12;

    /**
     * 取消自动连接
     */
    int BT_CANCEL_AUTO_LINK = 13;
}
