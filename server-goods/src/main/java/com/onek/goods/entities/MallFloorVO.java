package com.onek.goods.entities;

import java.util.List;

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
    private String otherParams;//其他属性
    private List<ProdVO> prodVOS;//楼层下的商品

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

    public String getOtherParams() {
        return otherParams;
    }

    public void setOtherParams(String otherParams) {
        this.otherParams = otherParams;
    }

    public List<ProdVO> getProdVOS() {
        return prodVOS;
    }

    public void setProdVOS(List<ProdVO> prodVOS) {
        this.prodVOS = prodVOS;
    }
}
