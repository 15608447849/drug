package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 企业经营范围
 * @time 2019/6/20 19:11
 **/
public class BusScopeVO {
    private int bscid;
    private int compid;
    private int busscope;
    private String codename;
    private int cstatus;

    public int getBscid() {
        return bscid;
    }

    public void setBscid(int bscid) {
        this.bscid = bscid;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public int getBusscope() {
        return busscope;
    }

    public void setBusscope(int busscope) {
        this.busscope = busscope;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getCodename() {
        return codename;
    }

    public void setCodename(String codename) {
        this.codename = codename;
    }
}
