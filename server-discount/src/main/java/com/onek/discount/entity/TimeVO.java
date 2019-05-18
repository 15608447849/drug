package com.onek.discount.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动场次
 * @time 2019/4/2 14:47
 **/
public class TimeVO {
    private String unqid;//编码
    private String actcode;//活动码
    private String sdate;//开始时间 hh:mm:ss
    private String edate;//结束时间 hh:mm:ss
    private int cstatus;

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getActcode() {
        return actcode;
    }

    public void setActcode(String actcode) {
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
