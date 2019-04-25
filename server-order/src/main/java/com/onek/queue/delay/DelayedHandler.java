package com.onek.queue.delay;

import java.util.Map;
import java.util.concurrent.*;

public class DelayedHandler<D extends IDelayedObject> implements IDelayedService<D> {
    private final ThreadPoolExecutor executor;
    private volatile CancelHandler cancelHndler;
    private volatile DelayQueue<DelayedObject> delayQueue;
    private Map<String, DelayedObject> objStore = new ConcurrentHashMap<>();
    private final TIME_TYPE time_type;
    private final long removeTime;
    private IDelayedHandler<D> handlerCall;

    public DelayedHandler(long delayTime, IDelayedHandler<D> handlerCall, TIME_TYPE time_type) {
        this.time_type = time_type;
        this.handlerCall = handlerCall;
        this.removeTime = convToRemoveTime(delayTime);

        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        this.delayQueue = new DelayQueue<>();

        executeThread();
    }

    public DelayedHandler(long delayTime, IDelayedHandler<D> handlerCall) {
        this(delayTime, handlerCall, TIME_TYPE.HOUR);
    }

    private void executeThread() {
        this.cancelHndler = new CancelHandler();
        this.cancelHndler.setDaemon(true);
        this.cancelHndler.start();
    }

    private long convToRemoveTime(long delayTime) {
        long result = delayTime;

        switch (this.time_type) {
            case DAY:
                result *= 24;
            case HOUR:
                result *= 60;
            case MINUTES:
                result *= 60;
            case SECOND:
                result *= 1000;
            case MILLSECOND:
                break;
        }

        return result;
    }

    protected void execute(Runnable runnable) {
        this.executor.execute(runnable);
    }

    @Override
    public void add(D delayed) {
        execute(() -> {
            if (delayed == null) {
                return ;
            }

            boolean addResult = addToQueue(delayed);

            if (addResult) {
                addSuccess(delayed);
            }
        });

    }

    protected boolean addToQueue(D delayed) {
        if (!objStore.containsKey(delayed.getUnqKey())) {
            DelayedObject obj = new DelayedObject(delayed);

            objStore.put(delayed.getUnqKey(), obj);

            return delayQueue.add(obj);
        }

        return false;
    }

    @Override
    public void remove(D delayed) {
        execute(() -> {
            if (delayed == null) {
                return ;
            }

            boolean removeResult = removeFromQueue(delayed.getUnqKey());

            if (removeResult) {
                removeSuccess(delayed);
            }
        });
    }

    public void removeByKey(String key) {
        DelayedObject delayed = this.objStore.get(key);

        if (delayed != null) {
            remove(delayed.obj);
        }
    }

    protected boolean removeFromQueue(String unqKey) {
        DelayedObject obj = this.objStore.remove(unqKey);

        return obj != null && delayQueue.remove(obj);
    }

    protected void addSuccess(D delayed) {}

    protected void removeSuccess(D delayed) {}

    protected void handlerSuccess(D delayed) { removeSuccess(delayed); }

    class CancelHandler extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    DelayedObject delayed = DelayedHandler.this.delayQueue.take();

                    if (delayed != null) {
                        // TODO 取消订单接口
                        boolean cancelResult =
                                DelayedHandler.this.handlerCall.handlerCall(delayed.obj);

                        if (cancelResult) {
                            DelayedHandler.this.handlerSuccess(delayed.obj);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class DelayedObject implements Delayed {
        private D obj;
        private long removeTime;

        public DelayedObject(D obj) {
            this.obj = obj;
            this.removeTime = DelayedHandler.this.removeTime + System.currentTimeMillis();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(
                    getRemoveTime() - System.currentTimeMillis(), TimeUnit.MICROSECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if (this == o) {
                return 0;
            }

            if (o instanceof DelayedHandler.DelayedObject) {
                DelayedObject cd = (DelayedObject) o;

                long diff = getRemoveTime() - cd.getRemoveTime();

                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }

            return -1;
        }

        public long getRemoveTime() {
            return this.removeTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DelayedObject that = (DelayedObject) o;

            return that.obj.getUnqKey().equals(this.obj.getUnqKey());
        }

        @Override
        public int hashCode() {
            return this.obj != null ? this.obj.getUnqKey().hashCode() : 0;
        }
    }

    public enum TIME_TYPE {
        DAY, HOUR, MINUTES, SECOND, MILLSECOND
    }
}
