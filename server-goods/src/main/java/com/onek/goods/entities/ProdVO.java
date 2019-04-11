package com.onek.goods.entities;

import redis.annation.DictCacheField;
import redis.annation.GetDictWay;

public class ProdVO {
    /* ----------- 商品SPU表 ------------- */
    private long spu;
    private String popname;
    private String prodname;
    private String standarNo;
    private long brandNo;
    private String brandName;
    private long manuNo;
    private String manuName;

    /* ----------- 商品SKU表 ------------- */
    private long sku;
    private double vatp;
    private double mp;
    private double rrp;
    private String vaildsdate;
    private String vaildedate;
    private String prodsdate;
    private String prodedate;

    private int store;
    private int activitystore;
    private int limits;
    private int sales;
    private int wholenum;
    private int medpacknum;
    private String spec;
    private int cstatus;

    @DictCacheField(reflectcolumn ="unitName")
    private int unit;
    private String unitName;
    private String imageUrl;

    // 活动消息
    private String sdate;
    private String edate;
    private int startnum;
    private int buynum;
    private int actlimit;

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

    public String getVaildsdate() {
        return vaildsdate;
    }

    public void setVaildsdate(String vaildsdate) {
        this.vaildsdate = vaildsdate;
    }

    public String getVaildedate() {
        return vaildedate;
    }

    public void setVaildedate(String vaildedate) {
        this.vaildedate = vaildedate;
    }

    public String getProdsdate() {
        return prodsdate;
    }

    public void setProdsdate(String prodsdate) {
        this.prodsdate = prodsdate;
    }

    public String getProdedate() {
        return prodedate;
    }

    public void setProdedate(String prodedate) {
        this.prodedate = prodedate;
    }

    public int getStore() {
        return store;
    }

    public void setStore(int store) {
        this.store = store;
    }

    public int getActivitystore() {
        return activitystore;
    }

    public void setActivitystore(int activitystore) {
        this.activitystore = activitystore;
    }

    public int getLimits() {
        return limits;
    }

    public void setLimits(int limits) {
        this.limits = limits;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public int getWholenum() {
        return wholenum;
    }

    public void setWholenum(int wholenum) {
        this.wholenum = wholenum;
    }

    public int getMedpacknum() {
        return medpacknum;
    }

    public void setMedpacknum(int medpacknum) {
        this.medpacknum = medpacknum;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSdate() {
        return sdate;
    }

    public void setSdate(String sdate) {
        this.sdate = sdate;
    }

    public String getEdate() {
        return edate;
    }

    public void setEdate(String edate) {
        this.edate = edate;
    }

    public int getStartnum() {
        return startnum;
    }

    public void setStartnum(int startnum) {
        this.startnum = startnum;
    }

    public int getBuynum() {
        return buynum;
    }

    public void setBuynum(int buynum) {
        this.buynum = buynum;
    }

    public int getActlimit() {
        return actlimit;
    }

    public void setActlimit(int actlimit) {
        this.actlimit = actlimit;
    }
}
