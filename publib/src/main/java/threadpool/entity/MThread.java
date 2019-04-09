package threadpool.entity;

import java.util.concurrent.LinkedBlockingQueue;

public class MThread extends Thread {
    private final LinkedBlockingQueue<Runnable> runQueue;
    private final int storeLimit;
    private long dleTime = System.currentTimeMillis();
    //是否运行
    private volatile boolean isRunning = false;
    //是否工作
    private volatile boolean isWorking = false;

    public MThread(String name,int storeLimit) {
        setName(name);
        this.storeLimit = storeLimit;
        this.runQueue = new LinkedBlockingQueue<>(storeLimit);
    }

    /**
     * 添加元素到队列中
     * 失败 返回这个run
     */
    public boolean addRunning(Runnable run){
        if (!isRunning) return false;
        if (storeLimit == 1 && isWorking) return false;
        if (runQueue.offer (run)){
            return true;
        }
        return false;
    }
    /**
     * 获取元素的当前大小
     */
    public int getQueueSize(){
        return runQueue.size();
    }
    /**
     * 移除并返问队列头部的元素
     * 如果队列为空，则阻塞
     */
    private Runnable getExecuteRunning(){
        try {
            return runQueue.take();
        } catch (InterruptedException e) {
//            e.printStackTrace();
        }
        return null;
    }

    //返回线程空闲时间
    public long getDleTime(){
        return isWorking?0L:(System.currentTimeMillis() - dleTime);
    }

    //运行
    public MThread  play(){
        if (!isRunning){
            isRunning = true;
            start();
        }
        return this;
    }


    //是否可结束
    public boolean over(){
        if (isRunning){
            if  (!runQueue.isEmpty()){
                return false;
            }
            isRunning = false;
            interrupt();//强制中断堵塞
        }
        return true;
    }

    /**
     * 如果队列存在元素,取出元素执行
     */
    @Override
    public void run() {

        while (isRunning){
            try {
                Runnable r = getExecuteRunning();
                if (r == null) continue;
                isWorking = true;
                dleTime = System.currentTimeMillis();
                try {
                    r.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                isWorking = false;
                dleTime = System.currentTimeMillis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
