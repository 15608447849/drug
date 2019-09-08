package com.onek.entity;

public class BDUserToCumultiveVO {

    private int uid; //用户码
    private int inviter;//所属用户id
    private String company ;//企业名
    private String cursorName ;//BD名称
    private String phone; //手机
    private String createdate;//审核日期
    private String createtime ;//审核时间
    private String caddrcode;//地区码
    private int control;//控销协议
    private String address;//收货地址
    public int status; //状态



    private String province; //省
    private String city; //市
    private String region; //区域

    public void setCompany(String company) {
        this.company = company;
    }

    public void setCursorName(String cursorName) {
        this.cursorName = cursorName;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompany() {
        return company;
    }

    public String getCursorName() {
        return cursorName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCreatedate() {
        return createdate;
    }

    public String getCreatetime() {
        return createtime;
    }

    public String getAddress() {
        return address;
    }

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
