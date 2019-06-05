package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IRowData;

public class CancelVolume implements IRowData, IColTotaler {
    private long areac;
    private String date;
    private double userCanceled;
    private double backCanceled;

    public double getUserCanceled() {
        return userCanceled;
    }

    public void setUserCanceled(double userCanceled) {
        this.userCanceled = userCanceled;
    }

    public double getBackCanceled() {
        return backCanceled;
    }

    public void setBackCanceled(double backCanceled) {
        this.backCanceled = backCanceled;
    }

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

    @Override
    public double getTotal() {
        return addDouble(userCanceled, backCanceled);
    }

    @Override
    public double[] getEachCol() {
        return new double[] { this.userCanceled, this.backCanceled, getTotal() };
    }
}
