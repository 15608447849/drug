package com.onek.calculate.entity;

import com.onek.calculate.service.AccurateMath;

import java.util.*;

public class Product extends AccurateMath implements IProduct {
    private long sku;
    private int nums;
    private double originalPrice;
    private double currentPrice;
    private double discounted;
    private Set<String> activityCodes = new HashSet<>();

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
        this.originalPrice = Math.max(originalPrice, 0);
        this.setCurrentPrice(this.originalPrice * nums);
    }

    public double getDiscounted() {
        return discounted;
    }

    public void setSku(long sku) { this.sku = sku; }

    public long getSKU() {
        return this.sku;
    }

    public int getNums() {
        return Math.max(this.nums, 0);
    }

    public double getOriginalPrice() {
        return Math.max(this.originalPrice, 0);
    }

    public double getCurrentPrice() {
        return Math.max(this.currentPrice, 0);
    }

    @Override
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = Math.max(currentPrice, 0);
    }

    public void addDiscounted(double discounted) {
        this.discounted = Math.min(add(this.discounted, discounted), mul(this.originalPrice, this.nums));
    }

    public void updateCurrentPrice() {
        this.currentPrice = sub(
                mul(this.originalPrice, this.nums), this.discounted);
    }

    @Override
    public void addActivityCode(String actCode) {
        activityCodes.add(actCode);
    }

    @Override
    public Collection<String> getActivityCodes() {
        return activityCodes;
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

    @Override
    public String toString() {
        return "Product{" +
                "sku=" + sku +
                ", nums=" + nums +
                ", originalPrice=" + originalPrice +
                ", currentPrice=" + currentPrice +
                ", discounted=" + discounted +
                '}';
    }
}
