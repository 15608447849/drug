package com.onek.discount.aaa.ccb.entity;

import com.onek.discount.aaa.ccb.service.AccurateMath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Product extends AccurateMath implements IProduct {
    private long sku;
    private int nums;
    private double originalPrice;
    private double discount;
    private double currentPrice;
    private long packageId;
    private Set<Long> activityList = new HashSet<>();
    private List<Gift> giftList = new ArrayList<Gift>();

    public long getSku() {
        return sku;
    }

    public void setNums(int nums) {
        this.nums = nums;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
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

    public List<Gift> getGiftList() {
        return giftList;
    }

    public void setGiftList(List<Gift> giftList) {
        this.giftList = giftList;
    }

    public void setSku(long sku) { this.sku = sku; }

    public long getSKU() {
        return this.sku;
    }

    public int getNums() {
        return this.nums;
    }

    public void addGift(Gift gift) {
        this.giftList.add(gift);
    }

    @Override
    public void addGifts(List<Gift> gifts) {
        this.giftList.addAll(gifts);
    }

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

    public void addSharePrice(double sharePrice) {
        this.discount = add(this.discount, sub(this.currentPrice, sharePrice));
    }

    public void updateCurrentPrice() {
        this.currentPrice = sub(mul(this.originalPrice, this.nums), this.discount);
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
