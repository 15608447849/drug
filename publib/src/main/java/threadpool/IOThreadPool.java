package threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class IOThreadPool extends Thread implements IThreadPool {

    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private ThreadPoolExecutor executor;
    private boolean isLoop = true;
    public IOThreadPool() {
        executor = createIoExecutor(1000);
        setDaemon(true);
        setName("IO线程池保留线程-"+getId());
        start();
    }
    //核心线程数,最大线程数,非核心线程空闲时间,存活时间单位,线程池中的任务队列
    private ThreadPoolExecutor createIoExecutor(int capacity) {

         return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                 capacity/2,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity),
                 r -> {
                     Thread thread = new Thread(r);
                     thread.setName("t-pio#-"+thread.getId());
                     return thread;
                 },
                 (r, executor) -> {
                     //超过IO线程池处理能力的任务,进入单线程执行队列
                     queue.offer(r);
                     synchronized (queue){
                         queue.notify();
                     }
                 }
         );
    }

    @Override
    public void run() {
        while (isLoop){
            try{
                //如果存在任务 , 一直执行 ,直到队列空, 进入等待执行
                Runnable runnable = queue.poll();
                if (runnable == null){
                    synchronized (queue){
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                runnable.run();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void post(Runnable runnable){
        executor.execute(runnable);
    }
    @Override
    public void close(){
        isLoop = false;
        if (executor!=null) executor.shutdownNow();
    }
}
