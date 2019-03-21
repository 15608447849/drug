package com.onek.user.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 收货人
 * @time 2019/3/21 13:44
 **/
public class ConsigneeVO {
    private int shipid;
    private int compid;
    private String contactname;
    private String contactphone;
    private int cstatus;

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
