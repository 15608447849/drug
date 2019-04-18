package com.onek.calculate.entity;

import com.onek.calculate.service.AccurateMath;

import java.util.HashSet;
import java.util.Set;

public class Product extends AccurateMath implements IProduct {
    private long sku;
    private int nums;
    private double originalPrice;
    private double currentPrice;
    private double discounted;
    private long packageId;
    private Set<Long> activityList = new HashSet<>();
//    private List<Gift> giftList = new ArrayList<Gift>();

    /**
    //商品标题
    private String ptitle;

    //规格
    private String spec;

    //厂商
    private String verdor;

    //有效期
    private String vperiod;

    //库存量
    private int inventory;
     */




    public long getSku() {
        return sku;
    }

    public void setNums(int nums) {
        this.nums = nums;
    }

    public void autoSetCurrentPrice(double originalPrice, int nums) {
        this.setOriginalPrice(originalPrice);
        this.setNums(nums);
        this.setCurrentPrice(originalPrice * nums);
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscounted() {
        return discounted;
    }

    public void setDiscounted(double discounted) {
        this.discounted = discounted;
    }

    public long getPackageId() {
        return packageId;
    }

    public void setPackageId(long packageId) {
        this.packageId = packageId;
    }

    public Set<Long> getActivityList() {
        return activityList;
    }

    public void setActivityList(Set<Long> activityList) {
        this.activityList = activityList;
    }

//    public List<Gift> getGiftList() {
//        return giftList;
//    }
//
//    public void setGiftList(List<Gift> giftList) {
//        this.giftList = giftList;
//    }

    public void setSku(long sku) { this.sku = sku; }

    public long getSKU() {
        return this.sku;
    }

    public int getNums() {
        return this.nums;
    }

//    public void addGift(Gift gift) {
//        this.giftList.add(gift);
//    }
//
//    @Override
//    public void addGifts(List<Gift> gifts) {
//        this.giftList.addAll(gifts);
//    }

    public void addActivity(long activity) {
        this.activityList.add(activity);
    }

    public double getOriginalPrice() {
        return this.originalPrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    @Override
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void addDiscounted(double discounted) {
        this.discounted = Math.min(add(this.discounted, discounted), getCurrentPrice());
    }

    public void updateCurrentPrice() {
        this.currentPrice = sub(
                mul(this.originalPrice, this.nums), this.discounted);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        return sku == product.sku;
    }

    @Override
    public int hashCode() {
        return (int) (sku ^ (sku >>> 32));
    }
}
