package com.conqueror.bluetoothphone.manager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池的代理,提交任务,执行任务,移除任务
 */
public class ThreadPoolProxy {

    ThreadPoolExecutor mExecutor;            // 只需要一个

    /*--------------- 以下参数直接决定了一个线程池,所以交给外部创建的时候定义 ---------------*/
    private int mCorePoolSize;
    private int mMaximumPoolSize;
    private long mKeepAliveTime;

    public ThreadPoolProxy(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        super();
        mCorePoolSize = corePoolSize;
        mMaximumPoolSize = maximumPoolSize;
        mKeepAliveTime = keepAliveTime;
    }

    /**
     * 初始化线程池
     */
    private synchronized void initThreadPoolExecutor() {// 双重检查加锁,只有在第一次实例化的时候才会启用同步机制
        TimeUnit unit = TimeUnit.MILLISECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler handler = new ThreadPoolExecutor.DiscardPolicy();
        if (mExecutor == null || mExecutor.isShutdown() || mExecutor.isTerminated()) {
            synchronized (ThreadPoolProxy.class) {
                if (mExecutor == null || mExecutor.isShutdown() || mExecutor.isTerminated()) {
                    mExecutor = new ThreadPoolExecutor(//
                            mCorePoolSize, // 核心线程数
                            mMaximumPoolSize, // 最大线程数
                            mKeepAliveTime, // 保持时间
                            unit, // 时间单位
                            workQueue,// 任务队列
                            threadFactory, // 线程工厂
                            handler// 异常部活期
                    );
                }
            }
        }
    }

    /**
     Future可以干嘛
     1. 判断任务是否执行完成
     2. 可以拿到任务执行完成之后的结果
     3. 可以捕获任务执行过程中抛出的异常
     */
    /**
     * 提交任务
     */
    public Future<?> submit(Runnable task) {
        initThreadPoolExecutor();
        return mExecutor.submit(task);
    }

    /**
     * 执行任务
     */
    public void execute(Runnable task) {
        initThreadPoolExecutor();
        mExecutor.execute(task);
    }

    /**
     * 移除任务
     */
    public void remove(Runnable task) {
        initThreadPoolExecutor();
        mExecutor.remove(task);
    }

}
