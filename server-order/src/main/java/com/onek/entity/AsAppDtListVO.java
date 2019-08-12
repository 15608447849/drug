package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName AsAppDtVO
 * @Description TODO
 * @date 2019-04-28 15:23
 */
public class AsAppDtListVO {


    private String orderno;//订单号
    private String asno = "0";//唯一码
    private int compid;//买家企业码
    private int astype;//售后类型(0 换货 1退款退货 2 仅退款 3 开发票)
    private int gstatus;//货物状态（0 已收到货  1 未收到货）
    private int ckstatus;//审核状态（-1拒绝 0 未审核 1审核通过）
    private int cstatus;//综合状态码
    private String apdata;//申请日期
    private String aptime;//申请时间
    private String checkern; //审核人
    private String contact;//联系人
    private String address; //收货地址
    private String compn; //门店
    private double refamt; //退款金额
    private int invoicetype; //退款金额
    private int pkgno;

    public int getPkgno() {
        return pkgno;
    }

    public void setPkgno(int pkgno) {
        this.pkgno = pkgno;
    }

    public int getInvoicetype() {
        return invoicetype;
    }

    public void setInvoicetype(int invoicetype) {
        this.invoicetype = invoicetype;
    }

    public String getCompn() {
        return compn;
    }

    public void setCompn(String compn) {
        this.compn = compn;
    }

    public double getRefamt() {
        return refamt;
    }

    public void setRefamt(double refamt) {
        this.refamt = refamt;
    }

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public String getAsno() {
        return asno;
    }

    public void setAsno(String asno) {
        this.asno = asno;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public int getAstype() {
        return astype;
    }

    public void setAstype(int astype) {
        this.astype = astype;
    }

    public int getGstatus() {
        return gstatus;
    }

    public void setGstatus(int gstatus) {
        this.gstatus = gstatus;
    }

    public int getCkstatus() {
        return ckstatus;
    }

    public void setCkstatus(int ckstatus) {
        this.ckstatus = ckstatus;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getApdata() {
        return apdata;
    }

    public void setApdata(String apdata) {
        this.apdata = apdata;
    }

    public String getAptime() {
        return aptime;
    }

    public void setAptime(String aptime) {
        this.aptime = aptime;
    }

    public String getCheckern() {
        return checkern;
    }

    public void setCheckern(String checkern) {
        this.checkern = checkern;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
