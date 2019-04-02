package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动场次
 * @time 2019/4/2 14:47
 **/
public class TimeVO {
    private long unqid;
    private long actcode;
    private String sdate;
    private String edate;
    private int cstatus;

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

    public String getSdate() {
        return sdate;
    }

    public void setSdate(String sdate) {
        this.sdate = sdate;
    }

    public String getEdate() {
        return edate;
    }

    public void setEdate(String edate) {
        this.edate = edate;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
