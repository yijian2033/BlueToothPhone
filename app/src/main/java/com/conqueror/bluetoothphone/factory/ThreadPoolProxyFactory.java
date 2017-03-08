package com.conqueror.bluetoothphone.factory;


import com.conqueror.bluetoothphone.manager.ThreadPoolProxy;

/**
 * 线程池代理的工厂类
 */
public class ThreadPoolProxyFactory {
    /**
     * 普通线程池代理(网络请求)
     */
    static ThreadPoolProxy mNormalThreadPoolProxy;    // 单例

    /**
     * 下载线程池代理(下载请求)
     */
    static ThreadPoolProxy mDownLoadThreadPoolProxy;

    /**
     * 返回普通线程池代理的实例
     *
     * @return
     */
    public static ThreadPoolProxy getNormalThreadPoolProxy() {

        if (mNormalThreadPoolProxy == null) {
            synchronized (ThreadPoolProxyFactory.class) {
                if (mNormalThreadPoolProxy == null) {
                    mNormalThreadPoolProxy = new ThreadPoolProxy(5, 5, 3000);
                }

            }
        }
        return mNormalThreadPoolProxy;
    }

    /**
     * 返回下载线程池代理的实例
     *
     * @return
     */
    public static ThreadPoolProxy getDownLoadThreadPoolProxy() {

        if (mDownLoadThreadPoolProxy == null) {
            synchronized (ThreadPoolProxyFactory.class) {
                if (mDownLoadThreadPoolProxy == null) {
                    mDownLoadThreadPoolProxy = new ThreadPoolProxy(3, 3, 3000);
                }
            }
        }
        return mDownLoadThreadPoolProxy;
    }
}
