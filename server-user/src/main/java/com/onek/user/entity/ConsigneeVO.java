package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 收货人
 * @time 2019/3/21 13:44
 **/
public class ConsigneeVO {
    private int shipid;//收货人id
    private int compid;//企业码
    private String contactname;//联系人
    private String contactphone;//联系人电话
    private int cstatus;//综合状态码 1删除 2默认

    public int getShipid() {
        return shipid;
    }

    public void setShipid(int shipid) {
        this.shipid = shipid;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public String getContactname() {
        return contactname;
    }

    public void setContactname(String contactname) {
        this.contactname = contactname;
    }

    public String getContactphone() {
        return contactphone;
    }

    public void setContactphone(String contactphone) {
        this.contactphone = contactphone;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
