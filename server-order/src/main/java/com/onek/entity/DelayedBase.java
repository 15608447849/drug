package com.onek.entity;

import com.onek.queue.delay.IDelayedObject;

public class DelayedBase implements IDelayedObject {
    private int compid;
    private String orderNo;

    public DelayedBase(int compid, String orderNo) {
        this.compid = compid;
        this.orderNo = orderNo;
    }

    public int getCompid() {
        return compid;
    }

    public String getOrderNo() {
        return orderNo;
    }

    @Override
    public String getUnqKey() {
        return this.orderNo;
    }
}
