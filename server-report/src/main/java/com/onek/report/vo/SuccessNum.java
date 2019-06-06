package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IRowData;

public class SuccessNum implements IColTotaler, IRowData {
    private long areac;
    private String date;
    private long successTotal;
    private long ret;
    private long back;

    public long getAreac() {
        return areac;
    }

    public void setAreac(long areac) {
        this.areac = areac;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getSuccessTotal() {
        return successTotal;
    }

    public void setSuccessTotal(long successTotal) {
        this.successTotal = successTotal;
    }

    public void addSuccessTotal(long successTotal) {
        this.successTotal = addLong(this.successTotal, successTotal);
    }

    public long getRet() {
        return ret;
    }

    public void setRet(long ret) {
        this.ret = ret;
    }

    public void addRet(long ret) {
        this.ret = addLong(ret, this.ret);
    }

    public long getBack() {
        return back;
    }

    public void setBack(long back) {
        this.back = back;
    }

    public void addBack(long back) {
        this.back = addLong(back, this.back);
    }

    public double getRetPercent() {
        return divDouble(100 * this.ret, getTotal());
    }

    public double getBackPercent() {
        return divDouble(100 * this.back, getTotal());
    }

    @Override
    public double[] getEachCol() {
        return new double[] {
                this.successTotal, this.ret, this.back,
                getRetPercent(), getBackPercent() } ;
    }

    @Override
    public double getTotal() {
        return this.successTotal;
    }
}
