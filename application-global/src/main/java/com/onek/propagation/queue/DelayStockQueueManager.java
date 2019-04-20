package com.onek.propagation.queue;

import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;

public class DelayStockQueueManager {

    private final static int DEFAULT_THREAD_NUM = 2;

    private static int thread_num = DEFAULT_THREAD_NUM;
    // 固定大小线程池
    private ExecutorService executor;
    // 守护线程
    private Thread daemonThread;
    // 延时队列
    private DelayQueue<DelayStockTask<?>> delayQueue;

//    private static final AtomicLong atomic = new AtomicLong(0);

    private static DelayStockQueueManager instance = new DelayStockQueueManager();

    private DelayStockQueueManager() {
        executor = Executors.newFixedThreadPool(thread_num);
        delayQueue = new DelayQueue<>();
        init();
    }

    public static DelayStockQueueManager getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init() {
        daemonThread = new Thread(() -> {
            execute();
        });
        daemonThread.setName("DelayQueueMonitor");
        daemonThread.start();
    }

    private void execute() {
        while (true) {
            Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
            System.out.println("############# 延时库存队列当前存活线程数量:" + map.size());
            int taskNum = delayQueue.size();
            System.out.println("######### 当前延时任务数量:" + taskNum);
            try {
                // 从延时队列中获取任务
                DelayStockTask<?> DelayStockTask = delayQueue.take();
                if (DelayStockTask != null) {
                    Runnable task = DelayStockTask.getTask();
                    if (null == task) {
                        continue;
                    }
                    // 提交到线程池执行task
                    executor.execute(task);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加任务
     *
     * @param task
     * @param time
     *            延时时间
     * @param unit
     *            时间单位
     */
    public void put(Runnable task, long time, TimeUnit unit) {
        // 获取延时时间
        long timeout = TimeUnit.NANOSECONDS.convert(time, unit);
        // 将任务封装成实现Delayed接口的消息体
        DelayStockTask<?> DelayStock = new DelayStockTask<>(timeout, task);
        // 将消息体放到延时队列中
        delayQueue.put(DelayStock);
    }

    /**
     * 删除任务
     *
     * @param task
     * @return
     */
    public boolean removeTask(DelayStockTask task) {

        return delayQueue.remove(task);
    }
}
