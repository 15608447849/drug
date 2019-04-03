package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 优惠商品赠换
 * @time 2019/4/2 14:50
 **/
public class AssGiftVO {

    private long unqid;//编码
    private long actcode;//活动码
    private long assgiftno;//赠换商品码
    private int cstatus;
    private String giftname;//商品名称

    public String getGiftname() {
        return giftname;
    }

    public void setGiftname(String giftname) {
        this.giftname = giftname;
    }

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getActcode() {
        return actcode;
    }

    public void setActcode(long actcode) {
        this.actcode = actcode;
    }

    public long getAssgiftno() {
        return assgiftno;
    }

    public void setAssgiftno(long assgiftno) {
        this.assgiftno = assgiftno;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
