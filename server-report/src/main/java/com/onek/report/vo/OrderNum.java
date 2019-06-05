package com.onek.report.vo;

import com.onek.report.core.IColTotaler;
import com.onek.report.core.IDoubleCal;
import com.onek.report.core.ILongCal;
import com.onek.report.core.IRowData;
import util.ArrayUtil;

public class OrderNum implements IRowData, IColTotaler {
    private CanceledNum canceledNum;
    private SuccessNum  successNum;

    public CanceledNum getCanceledNum() {
        return canceledNum;
    }

    public void setCanceledNum(CanceledNum canceledNum) {
        this.canceledNum = canceledNum;
    }

    public SuccessNum getSuccessNum() {
        return successNum;
    }

    public void setSuccessNum(SuccessNum successNum) {
        this.successNum = successNum;
    }

    public double getSuccessPercent() {
        return divDouble(successNum.getTotal(), getTotal());
    }

    @Override
    public double[] getEachCol() {
        double[] tempResult =
                ArrayUtil.concat(
                        this.canceledNum.getEachCol(),
                        this.successNum.getEachCol());

        return ArrayUtil.concat(tempResult,
                new double[] { getTotal(), getSuccessPercent() });
    }

    @Override
    public double getTotal() {
        return addDouble(canceledNum.getTotal(), successNum.getTotal());
    }
}
