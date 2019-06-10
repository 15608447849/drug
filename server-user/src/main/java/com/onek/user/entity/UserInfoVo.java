package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 用户
 * @time 2019/3/18 15:16
 **/
public class UserInfoVo {
//    private int oid;
    private int uid;//用户码
    private long uphone;//手机号
    private String uaccount;//账号
    private String urealname;//真实姓名
    private String upw;//密码
    private long roleid;//角色码
    private String adddate;//新增日期
    private String addtime;//新增时间
    private String offdate;//停用日期
    private String offtime;//停用时间
    private String ip;//最后登录ip
    private String logindate;//登录日期
    private String logintime;//登录时间
    private int cstatus;//综合状态码
    private String rname;//角色名称
    private String arean;//地区名称
    private int belong;//所属
    private int mroleid;
    private String arearng;
    private int cid;//企业码
    private int buid;
    private String buname;
    private String vcode;

    public String getVcode() {
        return vcode;
    }

    public void setVcode(String vcode) {
        this.vcode = vcode;
    }

    public int getBuid() {
        return buid;
    }

    public void setBuid(int buid) {
        this.buid = buid;
    }

    public String getBuname() {
        return buname;
    }

    public void setBuname(String buname) {
        this.buname = buname;
    }

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

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }


    public String getArearng() {
        return arearng;
    }

    public void setArearng(String arearng) {
        this.arearng = arearng;
    }
}
