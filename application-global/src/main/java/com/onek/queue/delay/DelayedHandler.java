package com.onek.queue.delay;

import java.util.Map;
import java.util.concurrent.*;

public class DelayedHandler<D extends IDelayedObject> implements IDelayedService<D> {
    private final ThreadPoolExecutor executor;
    private volatile CancelHandler cancelHndler;
    private volatile DelayQueue<DelayedObject<D>> delayQueue;
    private final Map<String, DelayedObject<D>> objStore = new ConcurrentHashMap<>();
    private final long delayTime;
    private final IDelayedHandler<D> handlerCall;

    public DelayedHandler(long delayTime, IDelayedHandler<D> handlerCall, TIME_TYPE time_type) {
        this.handlerCall = handlerCall;
        this.delayTime = convToMillsecond(delayTime, time_type);
        this.delayQueue = new DelayQueue<>();
        this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

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

    private static long convToMillsecond(long delayTime, TIME_TYPE time_type) {
        long result = delayTime;

        switch (time_type) {
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

            DelayedObject<D> obj = new DelayedObject(delayed, this.delayTime);

            boolean addResult = addToQueue(obj);

            if (addResult) {
                addSuccess(obj);
            }
        });

    }

    protected boolean addToQueue(DelayedObject<D> delayed) {
        if (!this.objStore.containsKey(delayed.getObj().getUnqKey())) {
            this.objStore.put(delayed.getObj().getUnqKey(), delayed);

            return delayQueue.add(delayed);
        }

        return false;
    }

    @Override
    public void remove(D delayed) {
        execute(() -> {
            if (delayed == null) {
                return ;
            }

            DelayedObject<D> obj = this.objStore.get(delayed.getUnqKey());

            if (obj == null) {
                return ;
            }

            boolean removeResult = removeFromQueue(delayed.getUnqKey());

            if (removeResult) {
                removeSuccess(obj);
            }
        });
    }

    public void removeByKey(String key) {
        DelayedObject<D> delayed = this.objStore.get(key);

        if (delayed != null) {
            remove(delayed.getObj());
        }
    }

    protected boolean removeFromQueue(String unqKey) {
        DelayedObject<D> obj = this.objStore.remove(unqKey);

        return obj != null && delayQueue.remove(obj);
    }

    protected void addSuccess(DelayedObject<D> delayed) {}

    protected void removeSuccess(DelayedObject<D> delayed) {}

    protected void handlerSuccess(DelayedObject<D> delayed) { removeSuccess(delayed); }

    class CancelHandler extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    DelayedObject<D> delayed = DelayedHandler.this.delayQueue.take();

                    if (delayed != null) {
                        boolean cancelResult =
                                DelayedHandler.this.handlerCall.
                                        handlerCall(delayed.getObj());

                        if (cancelResult) {
                            DelayedHandler.this.handlerSuccess(delayed);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public enum TIME_TYPE {
        DAY, HOUR, MINUTES, SECOND, MILLSECOND
    }

    protected static class DelayedObject<D extends IDelayedObject> implements Delayed {
        private D obj;
        private long removeTime;

        public DelayedObject() {}

        DelayedObject(D obj, long delayedTime) {
            this.obj = obj;
            this.removeTime = delayedTime + System.currentTimeMillis();
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

            if (o instanceof DelayedObject) {
                DelayedObject cd = (DelayedObject) o;

                long diff = getRemoveTime() - cd.getRemoveTime();

                return diff < 0 ? -1 : diff > 0 ? 1 : 0;
            }

            return -1;
        }

        public long getRemoveTime() {
            return this.removeTime;
        }

        public D getObj() {
            return this.obj;
        }

        public void setObj(D obj) {
            this.obj = obj;
        }

        public void setRemoveTime(long removeTime) {
            this.removeTime = removeTime;
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
}
