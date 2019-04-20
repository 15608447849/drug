package com.onek.propagation.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayStockTask<T extends Runnable> implements Delayed {

    private final long time;
    private final T task; // 任务类，也就是之前定义的任务类


    public DelayStockTask(long timeout, T task) {
        this.time = System.nanoTime() + timeout;
        this.task = task;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(this.time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        DelayStockTask other = (DelayStockTask) o;
        long diff = time - other.time;
        if (diff > 0) {
            return 1;
        } else if (diff < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    @Override
    public int hashCode() {
        return task.hashCode();
    }

    public T getTask() {
        return task;
    }
}
