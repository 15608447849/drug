package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 用户
 * @time 2019/3/18 15:16
 **/
public class UserInfoVo {
//    private int oid;
    private int uid;
    private long uphone;
    private String uaccount;
    private String urealname;
    private String upw;
    private long roleid;
    private String adddate;
    private String addtime;
    private String offdate;
    private String offtime;
    private String ip;
    private String logindate;
    private String logintime;
    private int cstatus;
    private String rname;
    private String arean;
    private int belong;
    private int mroleid;

    public int getMroleid() {
        return mroleid;
    }

    public void setMroleid(int mroleid) {
        this.mroleid = mroleid;
    }

    public int getBelong() {
        return belong;
    }

    public void setBelong(int belong) {
        this.belong = belong;
    }

    public String getArean() {
        return arean;
    }

    public void setArean(String arean) {
        this.arean = arean;
    }

    //    public int getOid() {
//        return oid;
//    }
//
//    public void setOid(int oid) {
//        this.oid = oid;
//    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public long getUphone() {
        return uphone;
    }

    public void setUphone(long uphone) {
        this.uphone = uphone;
    }

    public String getUaccount() {
        return uaccount;
    }

    public void setUaccount(String uaccount) {
        this.uaccount = uaccount;
    }

    public String getUrealname() {
        return urealname;
    }

    public void setUrealname(String urealname) {
        this.urealname = urealname;
    }

    public String getUpw() {
        return upw;
    }

    public void setUpw(String upw) {
        this.upw = upw;
    }

    public long getRoleid() {
        return roleid;
    }

    public void setRoleid(long roleid) {
        this.roleid = roleid;
    }

    public String getAdddate() {
        return adddate;
    }

    public void setAdddate(String adddate) {
        this.adddate = adddate;
    }

    public String getAddtime() {
        return addtime;
    }

    public void setAddtime(String addtime) {
        this.addtime = addtime;
    }

    public String getOffdate() {
        return offdate;
    }

    public void setOffdate(String offdate) {
        this.offdate = offdate;
    }

    public String getOfftime() {
        return offtime;
    }

    public void setOfftime(String offtime) {
        this.offtime = offtime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLogindate() {
        return logindate;
    }

    public void setLogindate(String logindate) {
        this.logindate = logindate;
    }

    public String getLogintime() {
        return logintime;
    }

    public void setLogintime(String logintime) {
        this.logintime = logintime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getRname() {
        return rname;
    }

    public void setRname(String rname) {
        this.rname = rname;
    }
}
