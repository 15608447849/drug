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

    public void setSku(long sku) { this.sku = sku; }

    public long getSKU() {
        return this.sku;
    }

    public int getNums() {
        return this.nums;
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
