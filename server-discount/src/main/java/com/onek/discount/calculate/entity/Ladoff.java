package com.onek.discount.calculate.entity;

import java.util.List;

public class Ladoff {
    private int oid;

    //阶梯码
    private long ladId;

    //金额阶梯值
    private double ladAmt;

    //数量阶梯值
    private int ladNum;

    //优惠码
    private int offerCode;

    //优惠值
    private double offerValue;

    // 综合状态
    private int cstatus;

    // 礼物列表
    private List<Gift> giftList;

    public int getOid() {
        return oid;
    }

    public void setOid(int oid) {
        this.oid = oid;
    }

    public long getLadId() {
        return ladId;
    }

    public void setLadId(long ladId) {
        this.ladId = ladId;
    }

    public double getLadAmt() {
        return ladAmt;
    }

    public void setLadAmt(double ladAmt) {
        this.ladAmt = ladAmt;
    }

    public int getLadNum() {
        return ladNum;
    }

    public void setLadNum(int ladNum) {
        this.ladNum = ladNum;
    }

    public int getOfferCode() {
        return offerCode;
    }

    public void setOfferCode(int offerCode) {
        this.offerCode = offerCode;
    }

    public double getOfferValue() {
        return offerValue;
    }

    public void setOfferValue(double offerValue) {
        this.offerValue = offerValue;
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
