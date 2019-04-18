package com.onek.entity;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName ShoppingCart
 * @Description TODO
 * @date 2019-04-15 14:06
 */
public class ShoppingCartDTO {

    //商品SKU
    private long pdno;

    private int pnum;

    private int compid;

    private int checked;

    private long unqid;

    private int cstatus;

    public int getChecked() {
        return checked;
    }

    public void setChecked(int checked) {
        this.checked = checked;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
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

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }
}
