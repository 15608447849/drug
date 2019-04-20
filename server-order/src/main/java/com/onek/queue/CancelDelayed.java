package com.onek.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CancelDelayed implements Delayed {
    private long removeTime;
    private String orderNo;
    private int compid;
    private static final long DELAY_TIME = 15 * 60 * 1000; //15分钟轮询

    public CancelDelayed(String orderNo, int compid) {
        this.removeTime = System.currentTimeMillis() + DELAY_TIME;
        this.orderNo = orderNo;
        this.compid = compid;
    }

    public CancelDelayed(String orderNo, int compid, long removeTime) {
        this.removeTime = removeTime;
        this.orderNo = orderNo;
        this.compid = compid;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(
                this.removeTime - System.currentTimeMillis(), TimeUnit.MICROSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (this == o) {
            return 0;
        }

        if (o instanceof CancelDelayed) {
            CancelDelayed cd = (CancelDelayed) o;

            long diff = this.getRemoveTime() - cd.getRemoveTime();

            return diff < 0 ? -1 : diff > 0 ? 1 : 0;
        }

        return -1;
    }

    public void setRemoveTime(long removeTime) {
        this.removeTime = removeTime;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public long getRemoveTime() {
        return removeTime;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public int getCompid() {
        return compid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null) return false;

        if (getClass() != o.getClass()) {
            if (o.getClass() == String.class) {
                return o.equals(this.orderNo);
            }
        }

        CancelDelayed that = (CancelDelayed) o;

        if (compid != that.compid) return false;

        return orderNo != null ? orderNo.equals(that.orderNo) : that.orderNo == null;
    }

    @Override
    public int hashCode() {
        int result = orderNo != null ? orderNo.hashCode() : 0;
        result = 31 * result + compid;
        return result;
    }

}
