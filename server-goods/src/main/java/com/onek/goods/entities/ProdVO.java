package com.onek.goods.entities;

import redis.annation.DictCacheField;
import util.TimeUtils;

import java.math.BigDecimal;
import java.util.List;

public class ProdVO {
    /* ----------- 商品SPU表 ------------- */
    private long spu;
    private String popname;
    private String prodname;
    private String standarNo;
    private String brandNo;
    private String brandName;
    private String manuNo;
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
    private int skuCstatus;;

    // 活动消息
    private boolean actprod;
    private boolean mutiact;
    private int rulestatus;
    private String sdate;
    private String edate;
    private int startnum;
    private int buynum;
    private int actlimit;
    private double actprize;
    private int actinitstock;
    private int surplusstock;
    private double minprize;
    private double maxprize;
    private String actcode;

    /** 动态属性 */
    //当前商品购物车数量 - 针对不同企业数值不同
    public int cart;
    private int consell;

    private int grossProfit;//毛利润
    private List<String> ladOffDesc;//毛利润

    private double purchaseprice;

    private int pkgprodnum;

    private boolean isneareffect;//是否未近效商品

    private boolean pkgUnEnough;

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

    public String getBrandNo() {
        return brandNo;
    }

    public void setBrandNo(String brandNo) {
        this.brandNo = brandNo;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getManuNo() {
        return manuNo;
    }

    public void setManuNo(String manuNo) {
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

    public double getActprize() {
        return actprize;
    }

    public void setActprize(double actprize) {
        this.actprize = actprize;
    }

    public int getRulestatus() {
        return rulestatus;
    }

    public void setRulestatus(int rulestatus) {
        this.rulestatus = rulestatus;
    }

    public boolean isActprod() {
        return actprod;
    }

    public void setActprod(boolean actprod) {
        this.actprod = actprod;
    }

    public boolean isMutiact() {
        return mutiact;
    }

    public void setMutiact(boolean mutiact) {
        this.mutiact = mutiact;
    }

    public double getMinprize() {
        return minprize;
    }

    public void setMinprize(double minprize) {
        this.minprize = minprize;
    }

    public double getMaxprize() {
        return maxprize;
    }

    public void setMaxprize(double maxprize) {
        this.maxprize = maxprize;
    }

    public String getActcode() {
        return actcode;
    }

    public void setActcode(String actcode) {
        this.actcode = actcode;
    }

    public int getSkuCstatus() {
        return skuCstatus;
    }

    public void setSkuCstatus(int skuCstatus) {
        this.skuCstatus = skuCstatus;
    }

    public int getActinitstock() {
        return actinitstock;
    }

    public void setActinitstock(int actinitstock) {
        this.actinitstock = actinitstock;
    }

    public void setSurplusstock(int surplusstock) {
        this.surplusstock = surplusstock;
    }

    public int getSurplusstock() {
        return surplusstock;
    }


    public int getConsell() {
        return consell;
    }

    public void setConsell(int consell) {
        this.consell = consell;
    }

    public List<String> getLadOffDesc() {
        return ladOffDesc;
    }

    public void setLadOffDesc(List<String> ladOffDesc) {
        this.ladOffDesc = ladOffDesc;
    }

    public int getCart() {
        return cart;
    }

    public void setCart(int cart) {
        this.cart = cart;
    }

    public void setGrossProfit(int grossProfit) {
        this.grossProfit = grossProfit;
    }

    public double getPurchaseprice() {
        return purchaseprice;
    }

    public void setPurchaseprice(double purchaseprice) {
        this.purchaseprice = purchaseprice;
    }

    public int getPkgprodnum() {
        return pkgprodnum;
    }

    public void setPkgprodnum(int pkgprodnum) {
        this.pkgprodnum = pkgprodnum;
    }

    public boolean isPkgUnEnough() {
        return pkgUnEnough;
    }

    public void setPkgUnEnough(boolean pkgUnEnough) {
        this.pkgUnEnough = pkgUnEnough;
    }
}
