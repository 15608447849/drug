package threadpool;


import threadpool.entity.MThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class MThreadPool extends Thread implements IThreadPool{

    private volatile boolean isWorking = false;

    private final ReentrantLock lock = new ReentrantLock();

    private final long TIME = 5 * 60 * 1000L;

    private final ArrayList<MThread> mThreadArrayList = new ArrayList<>();

    //每个线程 任务 存储上限(默认 1)
    private int count = 10;

    /**
     * cup支持的最大同时并发数 ( 最大: 核数 * 2 ) (默认 : 1)
     */
    private int simultaneously = Runtime.getRuntime().availableProcessors() * 8;

    private MThread overflow = new MThread("FTC@MT-OF-"+getId(),Integer.MAX_VALUE).play();

    public MThreadPool() {
        setName("FTC@MT-Pool-"+getId());
        start();
    }

    //设置每个线程的最大任务存储数量
    public MThreadPool setSingerThreadLimit(int count){
        if (mThreadArrayList.size()==0){ //如果mThread中的 线程对象 不为0  ,不可设置
            if (count <= 0){
                this.count = 1;
            }else{
                this.count = count;
            }
        }
        return this;
    }

    //设置最大并发数
    public MThreadPool setSimultaneously(int simultaneously) {
        if (simultaneously > Runtime.getRuntime().availableProcessors() * 2){
            this.simultaneously = Runtime.getRuntime().availableProcessors() * 2; //最大
        }else
        if (simultaneously <= 0){
            this.simultaneously = 1;
        }else{
            this.simultaneously =simultaneously;
        }
        return this;
    }



    @Override
    public void run() {
        while (isWorking){
            try {
                sleep(TIME);
                checkThreadAction();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        kill();
    }

    private void kill() {
        if (isWorking) return;
        isWorking = true;
        loopStopThread(overflow);
        Iterator<MThread> mThreadIterator  = mThreadArrayList.iterator();

        while (mThreadIterator.hasNext()){
            overflow = mThreadIterator.next();
            mThreadIterator.remove();
            loopStopThread(overflow);
        }
    }



    private void loopStopThread(MThread mt){
        while (!mt.over()) {
            try {
                sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    //移除空闲的多余线程
    private void checkThreadAction() {
        try{
            lock.lock();
            Iterator<MThread> mThreadIterator  = mThreadArrayList.iterator();
            MThread thread;
            while (mThreadIterator.hasNext()){
                thread = mThreadIterator.next();
                if (thread.getDleTime() > TIME){
                    if (thread.over()){
                        mThreadIterator.remove();
                    }
                }
            }
        }finally {
            lock.unlock();
        }
    }

    //外部调用
    private void addRunnable(Runnable run){
        try{
            lock.lock();
            if (mThreadArrayList.size()<simultaneously){
                addRunnable(run,0);
            }else if (mThreadArrayList.size() == simultaneously){
                //遍历添加
                Iterator<MThread> iterator = mThreadArrayList.iterator();
                MThread thread;
                while (iterator.hasNext()){
                    thread = iterator.next();
                    if (thread.addRunning(run)){
                        return;
                    }
                }
            }
            overflow.addRunning(run);
        }finally {
            lock.unlock();
        }
    }

    // 待启动任务到来
    private void addRunnable(Runnable run, int index){ // 从0层开始
        //判断下标是否当前最大并发数
        if ( index >= simultaneously){// 下标从0开始
            overflow.addRunning(run);
        }else{
            MThread thread = null;
            if (index == mThreadArrayList.size()){
                //创建,添加 MThread 线程
                thread = new MThread("FTC@MT-Sub-"+(index),count);
                if (mThreadArrayList.add(thread))  thread.play();
            }
            //获取 index映射的线程对象
            if (thread == null) thread = mThreadArrayList.get(index);
             //尝试添加
            if (!thread.addRunning(run)){
                //添加失败 , 队列满 ,向下层添加
                index++;
                addRunnable(run,index);
            }
        }
    }


    @Override
    public void post(Runnable runnable) {
        addRunnable(runnable);
    }

    @Override
    public void close() throws IOException {
     isWorking = false;
    }
}
