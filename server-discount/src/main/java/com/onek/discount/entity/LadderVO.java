package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LadderVO
 * @Description TODO
 * @date 2019-04-02 11:20
 */
public class LadderVO {


    //优惠码
    private long offerno;

    //规则码
    private int ruleno;

    //金额阶梯值
    private int ladamt;

    //数量阶梯值
    private int ladnum;

    //优惠值
    private int offer;

    //综合状态
    private int cstatus;

    public long getOfferno() {
        return offerno;
    }

    public void setOfferno(long offerno) {
        this.offerno = offerno;
    }

    public int getRuleno() {
        return ruleno;
    }

    public void setRuleno(int ruleno) {
        this.ruleno = ruleno;
    }

    public int getLadamt() {
        return ladamt;
    }

    public void setLadamt(int ladamt) {
        this.ladamt = ladamt;
    }

    public int getLadnum() {
        return ladnum;
    }

    public void setLadnum(int ladnum) {
        this.ladnum = ladnum;
    }

    public int getOffer() {
        return offer;
    }

    public void setOffer(int offer) {
        this.offer = offer;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
