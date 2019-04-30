package com.onek.util.member;

public class MemberEntity {
    private long unqid;
    private int compid;
    private int accupoints;
    private int balpoints;
    private int expirepoint;
//    private String createdate;
//    private String createtime;
    private int cstatus;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
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

//    public String getCreatedate() {
//        return createdate;
//    }
//
//    public void setCreatedate(String createdate) {
//        this.createdate = createdate;
//    }
//
//    public String getCreatetime() {
//        return createtime;
//    }
//
//    public void setCreatetime(String createtime) {
//        this.createtime = createtime;
//    }


    public int getExpirepoint() {
        return expirepoint;
    }

    public void setExpirepoint(int expirepoint) {
        this.expirepoint = expirepoint;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
