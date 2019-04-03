package com.onek.user.entity;

/**
 * @author Jiangwg
 * @version 1.1.1
 * @description 会员等级
 * @time 2019/4/3 10:45
 **/
public class MemberLevelVO {
    private long unqid;
    private String lname;
    private int groval;
    private int cstatus;

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public int getGroval() {
        return groval;
    }

    public void setGroval(int groval) {
        this.groval = groval;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
