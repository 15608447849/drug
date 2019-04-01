package com.onek.goods.entities;

public class ProdBrandVO {
    private int oid;
    private int brandno;
    private String brandname;
    private int cstatus;

    public int getOid() {
        return oid;
    }

    public void setOid(int oid) {
        this.oid = oid;
    }

    public int getBrandno() {
        return brandno;
    }

    public void setBrandno(int brandno) {
        this.brandno = brandno;
    }

    public String getBrandname() {
        return brandname;
    }

    public void setBrandname(String brandname) {
        this.brandname = brandname;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
