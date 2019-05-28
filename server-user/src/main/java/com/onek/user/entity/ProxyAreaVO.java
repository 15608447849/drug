package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 区域广播地区
 * @time 2019/5/28 16:34
 **/
public class ProxyAreaVO {

    private long unqid;
    private long msgid;
    private long areac;
    private int cstatus;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public long getMsgid() {
        return msgid;
    }

    public void setMsgid(long msgid) {
        this.msgid = msgid;
    }

    public long getAreac() {
        return areac;
    }

    public void setAreac(long areac) {
        this.areac = areac;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
