package com.onek.goods.entities;

/**
 * 楼层
 *
 * @Author jiangwg
 * @since 20190404
 * @version 1.0
 */
public class MallFloorVO {
    private int oid;
    private long unqid;
    private String fname;
    private int cstatus;

    public int getOid() {
        return oid;
    }

    public void setOid(int oid) {
        this.oid = oid;
    }

    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }
}
