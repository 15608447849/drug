package com.onek.user.entity;

/**
 * BD报表对象
 */
public class BDAchievementVO {
    private long uid; //用户id
    private long cid; //企业码
    private long roleid; //角色码
    private String urealname; //用户姓名
    private long belong; //用户所属
    private long flag; //用户针对对象 1-渠道总监，2-渠道经理，3-城市合伙人/城市经理，4-BMD ，5-BD


    public long getUid() {
        return uid;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }
    public long getCid() {
        return cid;
    }
    public void setCid(long cid) {
        this.cid = cid;
    }
    public long getRoleid() {
        return roleid;
    }
    public void setRoleid(long roleid) {
        this.roleid = roleid;
    }
    public String getUrealname() {
        return urealname;
    }
    public void setUrealname(String urealname) {
        this.urealname = urealname;
    }
    public long getBelong() {
        return belong;
    }
    public void setBelong(long belong) {
        this.belong = belong;
    }
    public long getFlag() {
        return flag;
    }
    public void setFlag(long flag) {
        this.flag = flag;
    }
}
