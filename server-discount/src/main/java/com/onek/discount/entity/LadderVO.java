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
    private int ladamt;

    //数量阶梯值
    private int ladnum;

    private int offercode;//优惠码

    //优惠值
    private int offer;

    //综合状态
    private int cstatus;

    private long assgiftno;//赠换商品码

    private String giftname;//商品名称

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
