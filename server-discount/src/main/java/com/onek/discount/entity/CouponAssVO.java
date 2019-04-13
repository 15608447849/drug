package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName CouponAssVO
 * @Description TODO
 * @date 2019-04-13 16:07
 */
public class CouponAssVO {

    //优惠券码
    private long coupno;

    //全局兼容码
    private int glbno;

    //优惠券规则码
    private int ruleno;

    //有效天数
    private int validday;

    //生效标记 0 即日生效、1 次日生效
    private int validflag;

    //综合状态
    private int cstatus;

    //规则名称
    private String rulename;

    //库存量
    private int actstock;

    //是否有使用要求
    private int reqflag;

    private String coupdesc;

    public String getCoupdesc() {
        return coupdesc;
    }

    public void setCoupdesc(String coupdesc) {
        this.coupdesc = coupdesc;
    }

    public int getReqflag() {
        return reqflag;
    }

    public void setReqflag(int reqflag) {
        this.reqflag = reqflag;
    }

    public long getCoupno() {
        return coupno;
    }

    public void setCoupno(long coupno) {
        this.coupno = coupno;
    }

    public int getGlbno() {
        return glbno;
    }

    public void setGlbno(int glbno) {
        this.glbno = glbno;
    }

    public int getRuleno() {
        return ruleno;
    }

    public void setRuleno(int ruleno) {
        this.ruleno = ruleno;
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

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getRulename() {
        return rulename;
    }

    public void setRulename(String rulename) {
        this.rulename = rulename;
    }

    public int getActstock() {
        return actstock;
    }

    public void setActstock(int actstock) {
        this.actstock = actstock;
    }
}
