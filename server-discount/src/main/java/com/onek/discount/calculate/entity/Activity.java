package com.onek.discount.calculate.entity;

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

    private double discounted;
    private List<IProduct> productList;
    private boolean freeShipping;
    private Map<Long, Integer> SKU_LIMITS = new HashMap<>();

    public Activity() {
        this.productList = new ArrayList<>();
    }

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

    private double[] getEachCurrent() {
        List<IProduct> prodList = getProductList();
        double[] results = new double[prodList.size()];

        for (int i = 0; i < results.length; i++) {
            results[i] = prodList.get(i).getCurrentPrice();
        }

        return results;
    }

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
    public boolean setExCoupon() {
        return this.excdiscount == 1;
    }

    @Override
    public void setLimits(long sku, int limits) {
        SKU_LIMITS.put(sku, limits);
    }

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
