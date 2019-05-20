package com.onek.entity;

/**
 * @author 11842
 * @version 1.1.1
 * @description 售后申请
 * @time 2019/4/23 14:19
 **/
public class AsAppVO {
    private String orderno;//订单号
    private long pdno;//商品码
    private String asno = "0";//唯一码
    private int compid;//买家企业码
    private int astype;//售后类型(0 换货 1退款退货 2 仅退款 3 开发票)
    private int gstatus;//货物状态（0 已收到货  1 未收到货）
    private int reason;//申请原因
    private int ckstatus;//审核状态（-1拒绝 0 未审核 1审核通过）
    private String ckdate;//审核日期
    private String cktime;//审核时间
    private String ckdesc;//审核说明
    private String invoice;//发票信息
    private int cstatus;//综合状态码
    private String apdata;//申请日期
    private String aptime;//申请时间
    private String apdesc;//退款描述
    private double refamt;//退款金额
    private int asnum;//退款数量

    private String brandname;
    private String manuname;
    private String prodname;
    private String spec;
    private String spu;
    private String reasonName;

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public long getPdno() {
        return pdno;
    }

    public void setPdno(long pdno) {
        this.pdno = pdno;
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

    public int getReason() {
        return reason;
    }

    public void setReason(int reason) {
        this.reason = reason;
    }

    public int getCkstatus() {
        return ckstatus;
    }

    public void setCkstatus(int ckstatus) {
        this.ckstatus = ckstatus;
    }

    public String getCkdate() {
        return ckdate;
    }

    public void setCkdate(String ckdate) {
        this.ckdate = ckdate;
    }

    public String getCktime() {
        return cktime;
    }

    public void setCktime(String cktime) {
        this.cktime = cktime;
    }

    public String getCkdesc() {
        return ckdesc;
    }

    public void setCkdesc(String ckdesc) {
        this.ckdesc = ckdesc;
    }

    public String getInvoice() {
        return invoice;
    }

    public void setInvoice(String invoice) {
        this.invoice = invoice;
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

    public String getApdesc() {
        return apdesc;
    }

    public void setApdesc(String apdesc) {
        this.apdesc = apdesc;
    }

    public double getRefamt() {
        return refamt;
    }

    public void setRefamt(double refamt) {
        this.refamt = refamt;
    }

    public int getAsnum() {
        return asnum;
    }

    public void setAsnum(int asnum) {
        this.asnum = asnum;
    }

    public String getProdname() {
        return prodname;
    }

    public void setProdname(String prodname) {
        this.prodname = prodname;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getBrandname() {
        return brandname;
    }

    public void setBrandname(String brandname) {
        this.brandname = brandname;
    }

    public String getManuname() {
        return manuname;
    }

    public void setManuname(String manuname) {
        this.manuname = manuname;
    }

    public String getSpu() {
        return spu;
    }

    public void setSpu(String spu) {
        this.spu = spu;
    }

    public String getReasonName() {
        return reasonName;
    }

    public void setReasonName(String reasonName) {
        this.reasonName = reasonName;
    }
}
