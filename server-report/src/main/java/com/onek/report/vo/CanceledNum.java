package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IRowData;

public class CanceledNum implements IColTotaler, IRowData {
    private long areac;
    private String date;
    private long userCanceled;
    private long backCanceled;

    public long getUserCanceled() {
        return userCanceled;
    }

    public void setUserCanceled(long userCanceled) {
        this.userCanceled = userCanceled;
    }

    public long getBackCanceled() {
        return backCanceled;
    }

    public void setBackCanceled(long backCanceled) {
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
    public double[] getEachCol() {
        return new double[] {
                this.userCanceled, this.backCanceled, getTotal() };
    }

    @Override
    public double getTotal() {
        return addLong(this.userCanceled, this.backCanceled);
    }
}
