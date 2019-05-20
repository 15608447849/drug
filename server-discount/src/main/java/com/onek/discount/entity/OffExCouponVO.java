package com.onek.discount.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName OffExCouponVO
 * @Description TODO
 * @date 2019-05-17 1:51
 */
public class OffExCouponVO {

    private String unqid;
    private String exno;
    private String expwd;
    private String coupno;
    private int cstatus;
    private String extime;

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }

    public String getExno() {
        return exno;
    }

    public void setExno(String exno) {
        this.exno = exno;
    }

    public String getExpwd() {
        return expwd;
    }

    public void setExpwd(String expwd) {
        this.expwd = expwd;
    }

    public String getCoupno() {
        return coupno;
    }

    public void setCoupno(String coupno) {
        this.coupno = coupno;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public String getExtime() {
        return extime;
    }

    public void setExtime(String extime) {
        this.extime = extime;
    }
}
