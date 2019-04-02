package com.onek.goods.entities;

public class ProdManuVO {
    private int oid;
    private long manuno;
    private String manuname;
    private int areac;
    private String address;
    private String createdate;
    private String createtime;
    private int cstatus;

    public int getOid() {
        return oid;
    }

    public void setOid(int oid) {
        this.oid = oid;
    }

    public long getManuno() {
        return manuno;
    }

    public void setManuno(long manuno) {
        this.manuno = manuno;
    }

    public String getManuname() {
        return manuname;
    }

    public void setManuname(String manuname) {
        this.manuname = manuname;
    }

    public int getAreac() {
        return areac;
    }

    public void setAreac(int areac) {
        this.areac = areac;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
}
