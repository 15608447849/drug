package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponPubLadderVO
 * @Description TODO
 * @date 2019-04-12 19:53
 */
public class CouponPubLadderVO {

    //金额阶梯值
    private double ladamt;

    //数量阶梯值
    private int ladnum;

    //优惠码
    private int offercode;

    //优惠值
    private double offer;

    //阶梯码
    private long unqid;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public double getLadamt() {
        return ladamt;
    }

    public void setLadamt(double ladamt) {
        this.ladamt = ladamt;
    }

    public int getLadnum() {
        return ladnum;
    }

    public void setLadnum(int ladnum) {
        this.ladnum = ladnum;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public double getOffer() {
        return offer;
    }

    public void setOffer(double offer) {
        this.offer = offer;
    }

    public int getOffercode() {
        return offercode;
    }
}