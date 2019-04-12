package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName LadderVO
 * @Description TODO
 * @date 2019-04-02 11:20
 */
public class LadderVO {

    //阶梯码
    private long unqid;

    //金额阶梯值
    private double ladamt;

    //数量阶梯值
    private int ladnum;

    private int offercode;//优惠码

    //优惠值
    private double offer;

    //综合状态
    private int cstatus;

    private long assgiftno;//赠换商品码

    private String giftname;//商品名称

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

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public int getOffercode() {
        return offercode;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public long getAssgiftno() {
        return assgiftno;
    }

    public void setAssgiftno(long assgiftno) {
        this.assgiftno = assgiftno;
    }

    public String getGiftname() {
        return giftname;
    }

    public void setGiftname(String giftname) {
        this.giftname = giftname;
    }
}
