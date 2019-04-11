package com.onek.util;

import threadpool.IOThreadPool;

/**
 * @Author: leeping
 * @Date: 2019/4/9 13:49
 */
public class IOThreadUtils {
    private static IOThreadPool pool = new IOThreadPool();
    public static void runTask(Runnable runnable){
        pool.post(runnable);
    }
}
