package com.onek.entity;

public class BDUserToCumultiveVO {

    public int uid; //用户码
    public int inviter;//所属用户id
    public String cname;//企业名
    public String urealname;//BD名称
    public String phone; //手机
    public String auditdate;//审核日期
    public String audittime;//审核时间
    public String caddrcode;//地区码
    private int control;//控销协议
    public int status; //状态



    private String province; //省
    private String city; //市
    private String region; //区域

    public void setProvince(String province) {
        this.province = province;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public int getUid() {
        return uid;
    }

    public int getInviter() {
        return inviter;
    }

    public String getCname() {
        return cname;
    }

    public String getUrealname() {
        return urealname;
    }

    public String getPhone() {
        return phone;
    }

    public String getAuditdate() {
        return auditdate;
    }

    public String getAudittime() {
        return audittime;
    }

    public String getCaddrcode() {
        return caddrcode;
    }

    public int getControl() {
        return control;
    }

    public int getStatus() {
        return status;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public void setInviter(int inviter) {
        this.inviter = inviter;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public void setUrealname(String urealname) {
        this.urealname = urealname;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAuditdate(String auditdate) {
        this.auditdate = auditdate;
    }

    public void setAudittime(String audittime) {
        this.audittime = audittime;
    }

    public void setCaddrcode(String caddrcode) {
        this.caddrcode = caddrcode;
    }

    public void setControl(int control) {
        this.control = control;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
