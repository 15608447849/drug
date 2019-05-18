package com.onek.entity;


import java.util.List;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponPubListVO
 * @Description TODO
 * @date 2019-04-08 17:26
 */
public class CouponPubVO {

    private String unqid;
    /**
     * 优惠券码
     */
    private String coupno;

    /**
     * 企业码
     */
    private int compid;

    /**
     * 规则名称
     */
    private String rulename;

    /**
     * 规则码
     */
    private int brulecode;

    /**
     * 有效天数
     */
    private int validday;

    /**
     * 生效标记 0 即日生效 、1 次日生效
     */
    private int validflag;


    private int glbno;

    private int goods;

    private int ctype;

    private int reqflag;

    private String ladder;

    private String startdate;

    private double offerAmt;

    private int qlfno;

    private long qlfval;

    private String exno;

    public String getExno() {
        return exno;
    }

    public void setExno(String exno) {
        this.exno = exno;
    }

    public int getQlfno() {
        return qlfno;
    }

    public void setQlfno(int qlfno) {
        this.qlfno = qlfno;
    }

    public long getQlfval() {
        return qlfval;
    }

    public void setQlfval(long qlfval) {
        this.qlfval = qlfval;
    }

    public double getOfferAmt() {
        return offerAmt;
    }

    public void setOfferAmt(double offerAmt) {
        this.offerAmt = offerAmt;
    }

    public String getStartdate() {
        return startdate;
    }

    public void setStartdate(String startdate) {
        this.startdate = startdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    private String enddate;

    public String getLadder() {
        return ladder;
    }

    public void setLadder(String ladder) {
        this.ladder = ladder;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public int getReqflag() {
        return reqflag;
    }

    public void setReqflag(int reqflag) {
        this.reqflag = reqflag;
    }

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }


    public int getGlbno() {
        return glbno;
    }

    public void setGlbno(int glbno) {
        this.glbno = glbno;
    }

    public int getGoods() {
        return goods;
    }

    public void setGoods(int goods) {
        this.goods = goods;
    }

    /**
     * 阶梯
     */
    private List<CouponPubLadderVO> ladderVOS;

    public List<CouponPubLadderVO> getLadderVOS() {
        return ladderVOS;
    }

    public void setLadderVOS(List<CouponPubLadderVO> ladderVOS) {
        this.ladderVOS = ladderVOS;
    }

    public String getCoupno() {
        return coupno;
    }

    public void setCoupno(String coupno) {
        this.coupno = coupno;
    }

    public int getCompid() {
        return compid;
    }

    public void setCompid(int compid) {
        this.compid = compid;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

    public int getBrulecode() {
        return brulecode;
    }

    public void setBrulecode(int brulecode) {
        this.brulecode = brulecode;
    }

    public int getValidday() {
        return validday;
    }

    public void setValidday(int validday) {
        this.validday = validday;
    }

    public int getValidflag() {
        return validflag;
    }

    public void setValidflag(int validflag) {
        this.validflag = validflag;
    }

    public static void main(String[] args) {

        int a = 536862733 / 8192 % 65535;
        System.out.println(a);
      //  536862733
    }
}
