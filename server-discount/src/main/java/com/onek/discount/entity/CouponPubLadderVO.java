package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponPubLadderVO
 * @Description TODO
 * @date 2019-04-12 19:53
 */
public class CouponPubLadderVO {

    //金额阶梯值
    private int ladamt;

    //数量阶梯值
    private int ladnum;

    ;//优惠码
    private int offercode;

    //优惠值
    private int offer;

    //阶梯码
    private long unqid;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
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

    public int getOffercode() {
        return offercode;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public int getOffer() {
        return offer;
    }

    public void setOffer(int offer) {
        this.offer = offer;
    }
}
