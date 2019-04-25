package com.onek.queue.delay;

public interface IDelayedHandler<D> {
    boolean handlerCall(D delayed);
}
