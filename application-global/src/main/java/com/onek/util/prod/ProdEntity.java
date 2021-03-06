package com.onek.util.prod;

import redis.annation.DictCacheField;
import redis.annation.GetDictWay;
import util.ITreeNode;

import java.util.ArrayList;
import java.util.List;

public class ProdEntity {
    private long spu;
    private String popname;
    private String prodname;
    private String standarNo;
    private long brandNo;
    private String brandName;
    private long manuNo;
    private String manuName;

    @DictCacheField(reflectcolumn ="rxName")
    private int rx;

    @DictCacheField(reflectcolumn ="insuranceName")
    private int insurance;

    private int gspGMS;
    private int gspSC;
    private String detail;
    private String spuCstatus;

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
    private int limits;
    private int sales;
    private int wholenum;
    private int medpacknum;

    @DictCacheField(reflectcolumn ="unitName")
    private int unit;

    private String ondate;
    private String ontime;
    private String offdate;
    private String offtime;
    private String spec;

    private int prodstatus;
    private int imagestatus;
    private int skuCstatus;
    private int consell;

    // ----- 自定义位置 ------- //

    private long classNo;
    private String className;

    @DictCacheField(reflectcolumn ="formName", type = "dosageform", dictWay = GetDictWay.CUSTOMC_AND_TYPE)
    private int form;
    private String formName;
    private String rxName;
    private String insuranceName;
    private String unitName;
    private String erpcode;

    public String getErpcode() {
        return erpcode;
    }

    public void setErpcode(String erpcode) {
        this.erpcode = erpcode;
    }

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

    public int getRx() {
        return rx;
    }

    public void setRx(int rx) {
        this.rx = rx;
    }

    public int getInsurance() {
        return insurance;
    }

    public void setInsurance(int insurance) {
        this.insurance = insurance;
    }

    public int getGspGMS() {
        return gspGMS;
    }

    public void setGspGMS(int gspGMS) {
        this.gspGMS = gspGMS;
    }

    public int getGspSC() {
        return gspSC;
    }

    public void setGspSC(int gspSC) {
        this.gspSC = gspSC;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getSpuCstatus() {
        return spuCstatus;
    }

    public void setSpuCstatus(String spuCstatus) {
        this.spuCstatus = spuCstatus;
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

    public int getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = unit;
    }

    public String getOndate() {
        return ondate;
    }

    public void setOndate(String ondate) {
        this.ondate = ondate;
    }

    public String getOntime() {
        return ontime;
    }

    public void setOntime(String ontime) {
        this.ontime = ontime;
    }

    public String getOffdate() {
        return offdate;
    }

    public void setOffdate(String offdate) {
        this.offdate = offdate;
    }

    public String getOfftime() {
        return offtime;
    }

    public void setOfftime(String offtime) {
        this.offtime = offtime;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public int getProdstatus() {
        return prodstatus;
    }

    public void setProdstatus(int prodstatus) {
        this.prodstatus = prodstatus;
    }

    public int getImagestatus() {
        return imagestatus;
    }

    public void setImagestatus(int imagestatus) {
        this.imagestatus = imagestatus;
    }

    public int getSkuCstatus() {
        return skuCstatus;
    }

    public void setSkuCstatus(int skuCstatus) {
        this.skuCstatus = skuCstatus;
    }

    public long getClassNo() {
        return classNo;
    }

    public void setClassNo(long classNo) {
        this.classNo = classNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getRxName() {
        return rxName;
    }

    public void setRxName(String rxName) {
        this.rxName = rxName;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public int getConsell() {
        return consell;
    }

    public void setConsell(int consell) {
        this.consell = consell;
    }
}
