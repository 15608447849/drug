package com.onek.discount.entity;

import redis.annation.DictCacheField;
import redis.annation.GetDictWay;

public class GiftableProdVO {
    /* ----------- 商品SPU表 ------------- */
    private long spu;
    private String popname;   // 通用名
    private String prodname;  // 商品名
    private String standarNo;  // 批准文号
    private long brandNo;  // 品牌码
    private String brandName;
    private long manuNo; // 厂商码
    private String manuName;

    /* ----------- 商品SKU表 ------------- */
    private long sku;
    private double vatp;  // 含税价
    private double mp;  // 市场价
    private double rrp; // 零售价
    private String spec; // 规格
    private double wp; // 批发价

    public long getSpu() {
        return spu;
    }

    public void setSpu(long spu) {
        this.spu = spu;
    }

    public String getPopname() {
        return popname;
    }

    public void setPopname(String popname) {
        this.popname = popname;
    }

    public String getProdname() {
        return prodname;
    }

    public void setProdname(String prodname) {
        this.prodname = prodname;
    }

    public String getStandarNo() {
        return standarNo;
    }

    public void setStandarNo(String standarNo) {
        this.standarNo = standarNo;
    }

    public long getBrandNo() {
        return brandNo;
    }

    public void setBrandNo(long brandNo) {
        this.brandNo = brandNo;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public long getManuNo() {
        return manuNo;
    }

    public void setManuNo(long manuNo) {
        this.manuNo = manuNo;
    }

    public String getManuName() {
        return manuName;
    }

    public void setManuName(String manuName) {
        this.manuName = manuName;
    }

    public long getSku() {
        return sku;
    }

    public void setSku(long sku) {
        this.sku = sku;
    }

    public double getVatp() {
        return vatp;
    }

    public void setVatp(double vatp) {
        this.vatp = vatp;
    }

    public double getMp() {
        return mp;
    }

    public void setMp(double mp) {
        this.mp = mp;
    }

    public double getRrp() {
        return rrp;
    }

    public void setRrp(double rrp) {
        this.rrp = rrp;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public double getWp() {
        return wp;
    }

    public void setWp(double wp) {
        this.wp = wp;
    }
}

