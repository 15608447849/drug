package com.onek.entity;

import com.onek.calculate.entity.Gift;

import java.util.List;

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

    private String conpno = "0";

    private int pnum;

    private int compid;

    private int checked;

    private String unqid = "0";

    private int cstatus;

    private long areano;

    private double pdprice;

    private String pkgno;

    public String getPkgno() {
        return pkgno;
    }

    public void setPkgno(String pkgno) {
        this.pkgno = pkgno;
    }

    public double getPdprice() {
        return pdprice;
    }

    public void setPdprice(double pdprice) {
        this.pdprice = pdprice;
    }

    public long getAreano() {
        return areano;
    }

    public void setAreano(long areano) {
        this.areano = areano;
    }

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

    public String getConpno() {
        return conpno;
    }

    public void setConpno(String conpno) {
        this.conpno = conpno;
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

    public String getUnqid() {
        return unqid;
    }

    public void setUnqid(String unqid) {
        this.unqid = unqid;
    }
}
