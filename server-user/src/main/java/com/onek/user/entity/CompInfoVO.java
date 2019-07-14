package com.onek.user.entity;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 企业信息对象
 * @time 2019/6/20 19:00
 **/
public class CompInfoVO {
    private int cid;//企业码
    private String cname;//企业名称
    private long caddrcode;//地区码
    private String caddr;//详细地址
    private int inviter;//采集人用户uid
    private int storetype;
    private long uphone;
    private int cstatus;
    private int control;//控销
    private String taxpayer;
    private String bankers;
    private String account;
    private String email;
    private String contactname;
    private String contactphone;//联系人电话
    private InvoiceVO invoiceVO;//发票信息
    private List<AptitudeVO> aptitudeVOS;//资质信息
    private List<BusScopeVO> busScopeVOS;//企业经营范围
    private String caddrname;//地区名
    private String busScopeStr;//经营范围字符串


    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public long getCaddrcode() {
        return caddrcode;
    }

    public void setCaddrcode(long caddrcode) {
        this.caddrcode = caddrcode;
    }

    public String getCaddr() {
        return caddr;
    }

    public void setCaddr(String caddr) {
        this.caddr = caddr;
    }

    public int getStoretype() {
        return storetype;
    }

    public void setStoretype(int storetype) {
        this.storetype = storetype;
    }

    public long getUphone() {
        return uphone;
    }

    public void setUphone(long uphone) {
        this.uphone = uphone;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public InvoiceVO getInvoiceVO() {
        return invoiceVO;
    }

    public void setInvoiceVO(InvoiceVO invoiceVO) {
        this.invoiceVO = invoiceVO;
    }

    public List<AptitudeVO> getAptitudeVOS() {
        return aptitudeVOS;
    }

    public void setAptitudeVOS(List<AptitudeVO> aptitudeVOS) {
        this.aptitudeVOS = aptitudeVOS;
    }

    public List<BusScopeVO> getBusScopeVOS() {
        return busScopeVOS;
    }

    public void setBusScopeVOS(List<BusScopeVO> busScopeVOS) {
        this.busScopeVOS = busScopeVOS;
    }

    public int getInviter() {
        return inviter;
    }

    public void setInviter(int inviter) {
        this.inviter = inviter;
    }

    public String getCaddrname() {
        return caddrname;
    }

    public void setCaddrname(String caddrname) {
        this.caddrname = caddrname;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public String getBusScopeStr() {
        return busScopeStr;
    }

    public void setBusScopeStr(String busScopeStr) {
        this.busScopeStr = busScopeStr;
    }

    public String getTaxpayer() {
        return taxpayer;
    }

    public void setTaxpayer(String taxpayer) {
        this.taxpayer = taxpayer;
    }

    public String getBankers() {
        return bankers;
    }

    public void setBankers(String bankers) {
        this.bankers = bankers;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public int getControl() {
        return control;
    }

    public void setControl(int control) {
        this.control = control;
    }
}
