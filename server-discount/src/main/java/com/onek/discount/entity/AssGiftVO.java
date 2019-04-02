package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 优惠商品赠换
 * @time 2019/4/2 14:50
 **/
public class AssGiftVO {

    private long unqid;
    private long offerno;
    private long assgiftno;
    private int cstatus;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getOfferno() {
        return offerno;
    }

    public void setOfferno(long offerno) {
        this.offerno = offerno;
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
