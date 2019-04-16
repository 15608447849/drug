package com.onek.user.entity;

public class MemberVO {
    private int unqid;
    private int compid;
    private int accupoints;
    private int balpoints;
    private String createdate;
    private String createtime;
    private int cstatus;

    public int getUnqid() {
        return unqid;
    }

    public void setUnqid(int unqid) {
        this.unqid = unqid;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public int getAccupoints() {
        return accupoints;
    }

    public void setAccupoints(int accupoints) {
        this.accupoints = accupoints;
    }

    public int getBalpoints() {
        return balpoints;
    }

    public void setBalpoints(int balpoints) {
        this.balpoints = balpoints;
    }

    public String getCreatedate() {
        return createdate;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
