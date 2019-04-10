package com.onek.goods.entities;

import redis.annation.DictCacheField;
import redis.annation.GetDictWay;

public class BgProdVO implements Cloneable{
    /* ----------- 商品SPU表 ------------- */
    private Long spu;
    private String popname;
    private String prodname;
    private String standarNo;
    private Long brandNo;
    private String brandName;
    private Long manuNo;
    private String manuName;

    @DictCacheField(reflectcolumn ="rxName")
    private Integer rx;

    @DictCacheField(reflectcolumn ="insuranceName")
    private Integer insurance;

    private Integer gspGMS;
    private Integer gspSC;
    private String detail;
    private String spuCstatus;

    /* ----------- 商品SKU表 ------------- */
    private Long sku;
    private double vatp;
    private double mp;
    private double rrp;
    private String vaildsdate;
    private String vaildedate;
    private String prodsdate;
    private String prodedate;

    private Integer store;
    private Integer activitystore;
    private Integer limits;
    private Integer sales;
    private Integer wholenum;
    private Integer medpacknum;

    @DictCacheField(reflectcolumn ="unitName")
    private Integer unit;

    private String ondate;
    private String ontime;
    private String offdate;
    private String offtime;
    private String spec;

    private Integer prodstatus;
    private Integer imagestatus;
    private Integer skuCstatus;

    private Long classNo;
    private String className;

    @DictCacheField(reflectcolumn ="formName", type = "dosageform", dictWay = GetDictWay.CUSTOMC_AND_TYPE)
    private Integer form;
    private String formName;
    private String rxName;
    private String insuranceName;
    private String unitName;

    public Long getSpu() {
        return spu;
    }

    public void setSpu(Long spu) {
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

    public Long getBrandNo() {
        return brandNo;
    }

    public void setBrandNo(Long brandNo) {
        this.brandNo = brandNo;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Long getManuNo() {
        return manuNo;
    }

    public void setManuNo(Long manuNo) {
        this.manuNo = manuNo;
    }

    public String getManuName() {
        return manuName;
    }

    public void setManuName(String manuName) {
        this.manuName = manuName;
    }

    public Integer getRx() {
        return rx;
    }

    public void setRx(Integer rx) {
        this.rx = rx;
    }

    public String getRxName() {
        return rxName;
    }

    public void setRxName(String rxName) {
        this.rxName = rxName;
    }

    public Integer getInsurance() {
        return insurance;
    }

    public void setInsurance(Integer insurance) {
        this.insurance = insurance;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
    }

    public Integer getGspGMS() {
        return gspGMS;
    }

    public void setGspGMS(Integer gspGMS) {
        this.gspGMS = gspGMS;
    }

    public Integer getGspSC() {
        return gspSC;
    }

    public void setGspSC(Integer gspSC) {
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

    public Long getSku() {
        return sku;
    }

    public void setSku(Long sku) {
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

    public Integer getStore() {
        return store;
    }

    public void setStore(Integer store) {
        this.store = store;
    }

    public Integer getActivitystore() {
        return activitystore;
    }

    public void setActivitystore(Integer activitystore) {
        this.activitystore = activitystore;
    }

    public Integer getLimits() {
        return limits;
    }

    public void setLimits(Integer limits) {
        this.limits = limits;
    }

    public Integer getSales() {
        return sales;
    }

    public void setSales(Integer sales) {
        this.sales = sales;
    }

    public Integer getWholenum() {
        return wholenum;
    }

    public void setWholenum(Integer wholenum) {
        this.wholenum = wholenum;
    }

    public Integer getMedpacknum() {
        return medpacknum;
    }

    public void setMedpacknum(Integer medpacknum) {
        this.medpacknum = medpacknum;
    }

    public Integer getUnit() {
        return unit;
    }

    public void setUnit(Integer unit) {
        this.unit = unit;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
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

    public Integer getProdstatus() {
        return prodstatus;
    }

    public void setProdstatus(Integer prodstatus) {
        this.prodstatus = prodstatus;
    }

    public Integer getImagestatus() {
        return imagestatus;
    }

    public void setImagestatus(Integer imagestatus) {
        this.imagestatus = imagestatus;
    }

    public Integer getSkuCstatus() {
        return skuCstatus;
    }

    public void setSkuCstatus(Integer skuCstatus) {
        this.skuCstatus = skuCstatus;
    }

    public Long getClassNo() {
        return classNo;
    }

    public void setClassNo(Long classNo) {
        this.classNo = classNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getForm() {
        return form;
    }

    public void setForm(Integer form) {
        this.form = form;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
