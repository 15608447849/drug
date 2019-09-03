package com.onek.user.entity;

public class BDCompVO {
    private long compid;
    private long inviter;
    private int cstatus;

    public long getCompid() {
        return compid;
    }
    public void setCompid(long compid) {
        this.compid = compid;
    }
    public long getInviter() {
        return inviter;
    }
    public void setInviter(long inviter) {
        this.inviter = inviter;
    }
    public int getCstatus() {
        return cstatus;
    }
    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
