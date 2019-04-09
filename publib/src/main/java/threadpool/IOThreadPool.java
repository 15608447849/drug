package threadpool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class IOThreadPool implements IThreadPool {
    private ThreadPoolExecutor executor;
    private IOThreadPool next;
    public IOThreadPool() {
        executor = createIoExecutor(500);
    }
    //核心线程数,最大线程数,非核心线程空闲时间,存活时间单位,线程池中的任务队列
    private ThreadPoolExecutor createIoExecutor(int capacity) {

         return new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                200,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(capacity),
                 r -> {
                     Thread thread = new Thread(r);
                     thread.setName("t-pio#-"+thread.getId());
                     return thread;
                 },
                new RejectedExecutionHandler(){
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                        //超过IO线程池处理能力的任务,进入下一个线程池
                        if (next == null) {
                            next = new IOThreadPool();
                        }
                        next.post(r);
                    }
                }
        );
    }
    @Override
    public void post(Runnable runnable){
        executor.execute(runnable);
    }
    @Override
    public void close(){
        if (next!=null) next.close();
        if (executor!=null) executor.shutdownNow();
    }
}
