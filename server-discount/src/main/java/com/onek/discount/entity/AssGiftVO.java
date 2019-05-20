package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 优惠商品赠换
 * @time 2019/4/2 14:50
 **/
public class AssGiftVO {

    private String unqid;//编码
    private int offercode;//优惠码
    private String assgiftno;//赠换商品码
    private int cstatus;
    private String giftname;//商品名称

    public String getGiftname() {
        return giftname;
    }

    public void setGiftname(String giftname) {
        this.giftname = giftname;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public int getOffercode() {
        return offercode;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public String getAssgiftno() {
        return assgiftno;
    }

    public void setAssgiftno(String assgiftno) {
        this.assgiftno = assgiftno;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
