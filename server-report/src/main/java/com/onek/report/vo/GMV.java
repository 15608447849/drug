package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IRowData;
import util.ArrayUtil;

public class GMV implements IRowData, IColTotaler {
    private CancelVolume cancelVolume;
    private SuccessVolume successVolume;

    public CancelVolume getCancelVolume() {
        return cancelVolume;
    }

    public void setCancelVolume(CancelVolume cancelVolume) {
        this.cancelVolume = cancelVolume;
    }

    public SuccessVolume getSuccessVolume() {
        return successVolume;
    }

    public void setSuccessVolume(SuccessVolume successVolume) {
        this.successVolume = successVolume;
    }

    public double getSuccessPercent() {
        return divDouble(this.successVolume.getTotal(), getTotal());
    }

    @Override
    public double getTotal() {
        return addDouble(
                this.cancelVolume.getTotal(),
                this.successVolume.getTotal());
    }

    @Override
    public double[] getEachCol() {
        double[] tempResult =
                ArrayUtil.concat(
                        this.cancelVolume.getEachCol(),
                        this.successVolume.getEachCol());

        return ArrayUtil.concat(tempResult,
                new double[] { getTotal(), getSuccessPercent() });
    }
}
