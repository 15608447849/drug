package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponUseDTO
 * @Description TODO
 * @date 2019-04-20 17:09
 */
public class CouponUseDTO {

    private long pdno;

    private int pnum;

    private int compid;

    private double price;

    private double amt;

    private double samt;

    private String coupon;

    private double shipfee;

    private double skprice;

    private String pkgno = "0";

    private int pkgnum;

    public String getPkgno() {
        return pkgno;
    }

    public void setPkgno(String pkgno) {
        this.pkgno = pkgno;
    }

    public int getPkgnum() {
        return pkgnum;
    }

    public void setPkgnum(int pkgnum) {
        this.pkgnum = pkgnum;
    }

    public double getSkprice() {
        return skprice;
    }

    public void setSkprice(double skprice) {
        this.skprice = skprice;
    }

    public double getShipfee() {
        return shipfee;
    }

    public void setShipfee(double shipfee) {
        this.shipfee = shipfee;
    }

    public String getCoupon() {
        return coupon;
    }

    public void setCoupon(String coupon) {
        this.coupon = coupon;
    }

    private int flag;

    private int balway;

    public int getBalway() {
        return balway;
    }

    public void setBalway(int balway) {
        this.balway = balway;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public long getPdno() {
        return pdno;
    }

    public void setPdno(long pdno) {
        this.pdno = pdno;
    }

    public int getPnum() {
        return pnum;
    }

    public void setPnum(int pnum) {
        this.pnum = pnum;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getAmt() {
        return amt;
    }

    public void setAmt(double amt) {
        this.amt = amt;
    }

    public double getSamt() {
        return samt;
    }

    public void setSamt(double samt) {
        this.samt = samt;
    }
}
