package com.onek.queue.delay;

public interface IDelayedService<D extends IDelayedObject> {
    void add(D delayed);
    void remove(D delayed);
}
