package com.onek.calculate.entity;

import java.util.List;

public class Ladoff {
    private int oid;

    //阶梯码
    private long unqid;

    //金额阶梯值
    private double ladamt;

    //数量阶梯值
    private int ladnum;

    //优惠码
    private int offercode;

    //优惠值
    private double offer;

    // 综合状态
    private int cstatus;

    // 礼物列表
    private List<Gift> giftList;

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

    public int getOffercode() {
        return offercode;
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

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public List<Gift> getGiftList() {
        return giftList;
    }

    public void setGiftList(List<Gift> giftList) {
        this.giftList = giftList;
    }
}
