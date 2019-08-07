package com.onek.goods.entities;

import redis.annation.DictCacheField;
import redis.annation.GetDictWay;
import util.StringUtils;
import util.TimeUtils;

import java.math.BigDecimal;

public class BgProdVO implements Cloneable{
    /* ----------- 商品SPU表 ------------- */
    private long spu;
    private String popname;   // 通用名
    private String prodname;  // 商品名
    private String standarNo;  // 批准文号
    private long brandNo;  // 品牌码
    private String brandName;
    private long manuNo; // 厂商码
    private String manuName;

    @DictCacheField(reflectcolumn ="rxName")
    private int rx; // 处方药标记

    @DictCacheField(reflectcolumn ="insuranceName")
    private int insurance; // 医保药码

    private int gspGMS;  // GSP质量管理标准码
    private int gspSC;  // GSP存储条件码
    private String detail; // 商品详情
    private String spuCstatus;
    private String qsc;  // 商品名快找码
    private int busscope; // 经营范围
    private String busscopen;

    /* ----------- 商品SKU表 ------------- */
    private long sku;
    private double vatp;  // 含税价
    private double mp;  // 市场价
    private double rrp; // 零售价
    private String vaildsdate; // 有效期起
    private String vaildedate; // 有效期止
    private String prodsdate; // 生产日期起
    private String prodedate; // 生产日期止

    private int store;  // 库存
    private int limits;
    private int sales; // 销售量
    private int wholenum; // 件装量
    private int medpacknum; // 中包装

    @DictCacheField(reflectcolumn ="unitName")
    private int unit; // 单位

    private String ondate; // 上架日期
    private String ontime; // 上架时间
    private String offdate; // 下架日期
    private String offtime; // 下架时间
    private String spec; // 规格

    private int prodstatus; // 商品状态
    private int imagestatus; // 商品图片码
    private int skuCstatus;
    private int expmonth; // 有效期月数
    private double wp; // 批发价
    private int consell; //


    private long classNo;  // 类别码
    private String className;

    @DictCacheField(reflectcolumn ="formName", type = "dosageform", dictWay = GetDictWay.CUSTOMC_AND_TYPE)
    private int form; // 剂型码
    private String formName;
    private String rxName;
    private String insuranceName;
    private String unitName;
    private String erpcode; // erp唯一码


    private int grossProfit;//毛利润

    private boolean isneareffect;//是否未近效商品

    public boolean getIsneareffect() {
        return isneareffect;
    }

    public void setIsneareffect(String sDate) {
        String nowTime = TimeUtils.str2Ymd_After_Mouth(TimeUtils.getCurrentDate(),6);
        long nowTimes = TimeUtils.str_yMd_2Date(nowTime).getTime();

        long vailTimes = TimeUtils.str_yMd_2Date(sDate).getTime();
        if(nowTimes>=vailTimes){
            this.isneareffect = true;
        }else{
            this.isneareffect = false;
        }

    }
    public int getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(double rrprice,double vatprice) {
        //计算毛利润
        //（零售-含税）/零售
        if(rrprice>0 && vatprice>0) {
            BigDecimal rrp = new BigDecimal(rrprice); //零售价格
            BigDecimal vatp = new BigDecimal(vatprice); //含税价格
            BigDecimal groPro = rrp.subtract(vatp).divide(rrp,2,BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100));
            this.grossProfit = groPro.intValue();
        }else {
            this.grossProfit = 0;
        }
    }


    public int getConsell() {
        return consell;
    }

    public void setConsell(int consell) {
        this.consell = consell;
    }

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

    public String getRxName() {
        return rxName;
    }

    public void setRxName(String rxName) {
        this.rxName = rxName;
    }

    public int getInsurance() {
        return insurance;
    }

    public void setInsurance(int insurance) {
        this.insurance = insurance;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
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
        return Math.max(wholenum, 1);
    }

    public void setWholenum(int wholenum) {
        this.wholenum = wholenum;
    }

    public int getMedpacknum() {
        return Math.max(medpacknum, 1);
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
        return spec == null ? "" : spec;
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

    public int getExpmonth() {
        return expmonth;
    }

    public void setExpmonth(int expmonth) {
        this.expmonth = expmonth;
    }

    public double getWp() {
        return wp;
    }

    public void setWp(double wp) {
        this.wp = wp;
    }

    public String getQsc() {
        return qsc;
    }

    public void setQsc(String qsc) {
        this.qsc = qsc;
    }

    public int getBusscope() {
        return busscope;
    }

    public void setBusscope(int busscope) {
        this.busscope = busscope;
    }

    public String getBusscopen() {
        return busscopen;
    }

    public void setBusscopen(String busscopen) {
        this.busscopen = busscopen;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
