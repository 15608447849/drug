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

    private long coupon;

    private double shipfee;

    public double getShipfee() {
        return shipfee;
    }

    public void setShipfee(double shipfee) {
        this.shipfee = shipfee;
    }

    public long getCoupon() {
        return coupon;
    }

    public void setCoupon(long coupon) {
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
