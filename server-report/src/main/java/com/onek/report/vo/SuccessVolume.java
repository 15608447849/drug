package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IRowData;

public class SuccessVolume implements IRowData, IColTotaler {
    private long areac;
    private String date;
    private double successTotal;
    private double ret;

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

    public double getSuccessTotal() {
        return successTotal;
    }

    public void setSuccessTotal(double successTotal) {
        this.successTotal = successTotal;
    }

    public double getRet() {
        return ret;
    }

    public void setRet(double ret) {
        this.ret = ret;
    }

    public double getRetPercent() {
        return divDouble(ret, getTotal());
    }

    @Override
    public double getTotal() {
        return this.successTotal;
    }

    @Override
    public double[] getEachCol() {
        return new double[] {
            this.successTotal, this.ret, getRetPercent()
        };
    }
}
