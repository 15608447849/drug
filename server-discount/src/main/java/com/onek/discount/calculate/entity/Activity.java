package com.onek.discount.calculate.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.onek.discount.calculate.service.AccurateMath;
import com.onek.discount.util.DiscountUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Activity extends AccurateMath implements IDiscount {
    private int oid;
    private long unqid;//活动码
    private String actname;//活动名称
    private int incpriority;//互斥优先级
    private int cpriority;//兼容优先级
    private int qualcode;//资格码
    private int qualvalue;//资格值
    private String actdesc;//活动描述
    private int excdiscount;//排斥优惠券
    private int acttype;//活动周期类型
    private long actcycle;//活动周期
    private String sdate;//活动开始日期
    private String edate;//活动结束日期
    private long brulecode;//活动规则码
    private int cstatus;//综合状态码
    private int limitnum; // 限购量
    private String sTime; // 起始时间
    private String eTime; // 结束时间
    private double actPrice;

    private double discounted;
    private List<IProduct> productList;
    private boolean freeShipping;
    private Map<Long, Integer> SKU_LIMITS = new HashMap<>();

    private List<Gift> giftList = new ArrayList<Gift>();

    public Activity() {
        this.productList = new ArrayList<>();
    }

    @JSONField(serialize = false)
    public int getLimitnum() {
        return this.limitnum;
    }

    @Override
    public long getActNo() {
        return this.unqid;
    }

    @Override
    public int getActtype() {
        return this.acttype;
    }

    @Override
    public long getActcycle() {
        return this.actcycle;
    }

    @JSONField(serialize = false)
    @Override
    public int getIncpriority() {
        return this.incpriority;
    }

    @Override
    public int getQualcode() {
        return this.qualcode;
    }

    @Override
    public int getQualvalue() {
        return this.qualvalue;
    }

    @Override
    public long getBRule() {
        return this.brulecode;
    }

    public int getPriority() {
        return this.incpriority * 10 + this.cpriority;
    }

    @JSONField(serialize = false)
    private double[] getEachCurrent() {
        List<IProduct> prodList = getProductList();
        double[] results = new double[prodList.size()];

        for (int i = 0; i < results.length; i++) {
            results[i] = prodList.get(i).getCurrentPrice();
        }

        return results;
    }

    @JSONField(serialize = false)
    public void addDiscounted(double discount) {
        if (discount <= 0) {
            return;
        }

        this.discounted = add(this.discounted, discount);

        double[] shared = DiscountUtil.shareDiscount(getEachCurrent(), discount);

        List<IProduct> prodList = getProductList();

        for (int i = 0; i < shared.length; i++) {
            prodList.get(i).addSharePrice(shared[i]);
        }
    }

    @Override
    public void setDiscounted(double discount) {
        this.discounted = discount;
    }

    @Override
    public double getDiscounted() {
        return this.discounted;
    }

    public void addProduct(IProduct product) {
        this.productList.add(product);
    }

    @Override
    public List<IProduct> getProductList() {
        return this.productList;
    }

    @Override
    public void setFreeShipping(boolean free) {
        this.freeShipping = free;
    }

    @Override
    public boolean getFreeShipping() {
        return this.freeShipping;
    }

    @Override
    public void setExCoupon(boolean exCoupon) {
        this.excdiscount = exCoupon ? 1 : 0;
    }

    @Override
    public boolean getExCoupon() {
        return this.excdiscount == 1;
    }

    @Override
    public void setLimits(long sku, int limits) {
        SKU_LIMITS.put(sku, limits);
    }

    @JSONField(serialize = false)
    @Override
    public int getLimits(long sku) {
        Integer result = SKU_LIMITS.get(sku);

        return result == null ? 0 : result;
    }

    @Override
    public String getStartTime() {
        return this.sTime;
    }

    @Override
    public String getEndTime() {
        return this.eTime;
    }

    @Override
    public void addGift(Gift gift) {
        giftList.add(gift);
    }

    @Override
    public void addGifts(List<Gift> gifts) {
        giftList.addAll(gifts);
    }

    public String getsTime() {
        return sTime;
    }

    public void setsTime(String sTime) {
        this.sTime = sTime;
    }

    public String geteTime() {
        return eTime;
    }

    public void seteTime(String eTime) {
        this.eTime = eTime;
    }

    @JSONField(serialize = false)
    public int getOid() {
        return oid;
    }

    public void setOid(int oid) {
        this.oid = oid;
    }

    @JSONField(serialize = false)
    public long getUnqid() {
        return unqid;
    }

    public void setUnqid(long unqid) {
        this.unqid = unqid;
    }

    public String getActname() {
        return actname;
    }

    public void setActname(String actname) {
        this.actname = actname;
    }

    public void setIncpriority(int incpriority) {
        this.incpriority = incpriority;
    }

    @JSONField(serialize = false)
    public int getCpriority() {
        return cpriority;
    }

    public void setCpriority(int cpriority) {
        this.cpriority = cpriority;
    }

    public void setQualcode(int qualcode) {
        this.qualcode = qualcode;
    }

    public void setQualvalue(int qualvalue) {
        this.qualvalue = qualvalue;
    }

    public String getActdesc() {
        return actdesc;
    }

    public void setActdesc(String actdesc) {
        this.actdesc = actdesc;
    }

    public int getExcdiscount() {
        return excdiscount;
    }

    public void setExcdiscount(int excdiscount) {
        this.excdiscount = excdiscount;
    }

    public void setActtype(int acttype) {
        this.acttype = acttype;
    }

    public void setActcycle(long actcycle) {
        this.actcycle = actcycle;
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

    public long getBrulecode() {
        return brulecode;
    }

    public void setBrulecode(long brulecode) {
        this.brulecode = brulecode;
    }

    public int getCstatus() {
        return cstatus;
    }

    public void setCstatus(int cstatus) {
        this.cstatus = cstatus;
    }

    public void setLimitnum(int limitnum) {
        this.limitnum = limitnum;
    }

    public void setProductList(List<IProduct> productList) {
        this.productList = productList;
    }

    public boolean isFreeShipping() {
        return freeShipping;
    }

    public double getActPrice() {
        return actPrice;
    }

    public void setActPrice(double actPrice) {
        this.actPrice = actPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Activity activity = (Activity) o;

        return unqid == activity.unqid;
    }

    @Override
    public int hashCode() {
        return (int) (unqid ^ (unqid >>> 32));
    }
}
