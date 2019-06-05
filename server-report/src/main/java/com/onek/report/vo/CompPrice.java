package com.onek.report.vo;

import com.onek.report.core.IRowData;

public class CompPrice implements IRowData {
    private long areac;
    private String date;
    private long max;
    private long min;
    private double avg;

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
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
        return new double[] { this.max, this.max, this.avg };
    }
}
