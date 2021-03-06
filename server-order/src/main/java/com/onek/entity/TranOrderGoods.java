package com.onek.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单关联商品信息
 * @time 2019/4/16 15:15
 **/
public class TranOrderGoods {
    private String unqid;//全局唯一
    private String orderno;//订单号
    private int compid;//企业码
    private long pdno;//商品码（SKU）
    private double pdprice;//商品单价
    private double distprice;//商品优惠价
    private double payamt;//支付金额
    private double coupamt;//优惠券金额
    private int promtype;//促销类型
    private int pkgno;//套餐码
    private int asstatus;//售后状态
    private String createdate;//创建日期
    private String createtime;//创建时间
    private int cstatus;
    private int pnum;//商品数量
    private String actcode;//活动码(前台传数组)
    private double balamt; // 余额

    private String pname;  // 商品名
    private String pspec; // 商品规格
    private String manun; // 厂商名
    private String spu; // spu
    private String standarNo;
    private String brandn;
    private double vatp;
    private double mp;
    private double rrp;

    private boolean isGift;
    private List<TranOrderGoods> pkgGoods;

    public void setPkgGoods(List<TranOrderGoods> pkgGoods) {
        this.pkgGoods = pkgGoods;
    }

    public List<TranOrderGoods> getPkgGoods() {
        return pkgGoods;
    }

    public boolean isGift() {
        return isGift;
    }

    public void setGift(boolean gift) {
        isGift = gift;
    }

    public double getVatp() {
        return vatp;
    }

    public void setVatp(double vatp) {
        this.vatp = vatp;
    }

    public double getMp() {
        return mp;
    }

    public void setMp(double mp) {
        this.mp = mp;
    }

    public double getRrp() {
        return rrp;
    }

    public void setRrp(double rrp) {
        this.rrp = rrp;
    }

    public String getBrandn() {
        return brandn;
    }

    public void setBrandn(String brandn) {
        this.brandn = brandn;
    }

    public String getStandarNo() {
        return standarNo;
    }

    public void setStandarNo(String standarNo) {
        this.standarNo = standarNo;
    }

    public double getBalamt() {
        return balamt;
    }

    public void setBalamt(double balamt) {
        this.balamt = balamt;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getOrderno() {
        return orderno;
    }

    public void setOrderno(String orderno) {
        this.orderno = orderno;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public long getPdno() {
        return pdno;
    }

    public void setPdno(long pdno) {
        this.pdno = pdno;
    }

    public double getPdprice() {
        return pdprice;
    }

    public void setPdprice(double pdprice) {
        this.pdprice = pdprice;
    }

    public double getDistprice() {
        return distprice;
    }

    public void setDistprice(double distprice) {
        this.distprice = distprice;
    }

    public double getPayamt() {
        return payamt;
    }

    public void setPayamt(double payamt) {
        this.payamt = payamt;
    }

    public double getCoupamt() {
        return coupamt;
    }

    public void setCoupamt(double coupamt) {
        this.coupamt = coupamt;
    }

    public int getPromtype() {
        return promtype;
    }

    public void setPromtype(int promtype) {
        this.promtype = promtype;
    }

    public int getPkgno() {
        return pkgno;
    }

    public void setPkgno(int pkgno) {
        this.pkgno = pkgno;
    }

    public int getAsstatus() {
        return asstatus;
    }

    public void setAsstatus(int asstatus) {
        this.asstatus = asstatus;
    }

    public String getCreatedate() {
        return createdate;
    }

    public void setCreatedate(String createdate) {
        this.createdate = createdate;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public int getPnum() {
        return pnum;
    }

    public void setPnum(int pnum) {
        this.pnum = pnum;
    }

    public String getPname() {
        return pname;
    }

    public void setPname(String pname) {
        this.pname = pname;
    }

    public String getPspec() {
        return pspec;
    }

    public void setPspec(String pspec) {
        this.pspec = pspec;
    }

    public String getManun() {
        return manun;
    }

    public void setManun(String manun) {
        this.manun = manun;
    }

    public String getActcode() {
        return actcode;
    }

    public void setActcode(String actcode) {
        this.actcode = actcode;
    }

    //    public String getActCodeStr() {
//        return actCodeStr;
//    }
//
//    public void setActCodeStr(String actCodeStr) {
//        this.actCodeStr = actCodeStr;
//    }

    public String getSpu() {
        return spu;
    }

    public void setSpu(String spu) {
        this.spu = spu;
    }

    @Override
    public String toString() {
        return "TranOrderGoods{" +
                "unqid='" + unqid + '\'' +
                ", orderno='" + orderno + '\'' +
                ", compid=" + compid +
                ", pdno=" + pdno +
                ", pdprice=" + pdprice +
                ", distprice=" + distprice +
                ", payamt=" + payamt +
                ", coupamt=" + coupamt +
                ", promtype=" + promtype +
                ", pkgno=" + pkgno +
                ", asstatus=" + asstatus +
                ", createdate='" + createdate + '\'' +
                ", createtime='" + createtime + '\'' +
                ", cstatus=" + cstatus +
                ", pnum=" + pnum +
                ", actcode='" + actcode + '\'' +
                ", balamt=" + balamt +
                ", pname='" + pname + '\'' +
                ", pspec='" + pspec + '\'' +
                ", manun='" + manun + '\'' +
                ", spu='" + spu + '\'' +
                ", standarNo='" + standarNo + '\'' +
                ", brandn='" + brandn + '\'' +
                ", vatp=" + vatp +
                ", mp=" + mp +
                ", rrp=" + rrp +
                '}';
    }
}
