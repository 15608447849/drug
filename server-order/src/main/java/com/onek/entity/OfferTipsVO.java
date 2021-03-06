package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName OfferTipsVO
 * @Description TODO
 * @date 2019-05-14 11:22
 */
public class OfferTipsVO {

    /**
     * 阶梯金额
     */
    private double ladamt;

    /**
     * 数量阶梯值
     */
    private int ladnum;

    /**
     * 优惠码
     */
    private int offercode;

    /**
     * 活动名称
     */
    private String offername;

    /**
     * 优惠值
     */
    private double offer;

    /**
     * 阶梯码
     */
    private String unqid = "0";

    /**
     * 下一个阶梯数量
     */
    private int nladnum;

    /**
     * 下一个阶梯金额
     */
    private double nladamt;

    /**
     * 下一个阶梯优惠值
     */
    private double noffer;

    private double gapamt;

    private int gapnum;

    private String currladDesc;

    private String nextladDesc;

    public String getNextladDesc() {
        return nextladDesc;
    }

    public void setNextladDesc(String nextladDesc) {
        this.nextladDesc = nextladDesc;
    }

    public String getCurrladDesc() {
        return currladDesc;
    }

    public void setCurrladDesc(String currladDesc) {
        this.currladDesc = currladDesc;
    }

    public double getGapamt() {
        return gapamt;
    }

    public void setGapamt(double gapamt) {
        this.gapamt = gapamt;
    }

    public int getGapnum() {
        return gapnum;
    }

    public void setGapnum(int gapnum) {
        this.gapnum = gapnum;
    }

    public String getOffername() {
        return offername;
    }

    public void setOffername(String offername) {
        this.offername = offername;
    }

    public double getLadamt() {
        return ladamt;
    }

    public void setLadamt(double ladamt) {
        this.ladamt = ladamt;
    }

    public int getLadnum() {
        return ladnum;
    }

    public void setLadnum(int ladnum) {
        this.ladnum = ladnum;
    }

    public int getOffercode() {
        return offercode;
    }

    public void setOffercode(int offercode) {
        this.offercode = offercode;
    }

    public double getOffer() {
        return offer;
    }

    public void setOffer(double offer) {
        this.offer = offer;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public int getNladnum() {
        return nladnum;
    }

    public void setNladnum(int nladnum) {
        this.nladnum = nladnum;
    }

    public double getNladamt() {
        return nladamt;
    }

    public void setNladamt(double nladamt) {
        this.nladamt = nladamt;
    }

    public double getNoffer() {
        return noffer;
    }

    public void setNoffer(double noffer) {
        this.noffer = noffer;
    }
}
