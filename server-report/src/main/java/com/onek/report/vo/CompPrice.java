package com.onek.report.vo;

import com.onek.report.core.IDoubleCal;
import com.onek.report.core.IRowData;

public class CompPrice implements IRowData, IDoubleCal {
    private long areac;
    private String date;
    private double max;
    private double min;
    private double avg;

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public void addMax(double max) {
        this.max = Math.max(max, this.max);
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public void addMin(double min) {
        this.min = Math.min(min, this.min);
    }

    public double getAvg() {
        return avg;
    }

    public void setAvg(double avg) {
        this.avg = avg;
    }

    public void addAvg(double avg) {
        this.avg = divDouble(addDouble(avg, this.avg), 2);
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
        return new double[] { this.max, this.min, this.avg };
    }
}
