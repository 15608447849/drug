package com.onek.calculate.entity;

import com.onek.calculate.service.AccurateMath;
import com.onek.calculate.util.DiscountUtil;
import org.apache.poi.ss.formula.functions.T;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Package extends AccurateMath implements IProduct {
    private long packageId;
    private int nums;
    private double originalPrice;
    private double currentPrice;
    private double discounted;
    private List<Product> productList = new ArrayList<>();
    private int expireFlag = -2;  // -2：套餐被过滤； -1：无此套餐； 0：正常

    public long getPackageId() {
        return packageId;
    }

    public void setPackageId(long packageId) {
        this.packageId = packageId;
    }


    public long getSku() {
        return packageId;
    }

    public void setNums(int nums) {
        this.nums = nums;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = Math.max(originalPrice, 0);
        this.setCurrentPrice(this.originalPrice * nums);
    }

    public double getDiscounted() {
        return discounted;
    }

    @Override
    public long getSKU() {
        return packageId;
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
//        double d = Math.min(add(this.discounted, discounted), mul(this.originalPrice, this.nums));

        double[] shared = DiscountUtil.shareDiscount(
                DiscountUtil.getEachCurrent(this.productList), discounted);

        Product product;
        double befDiscounted, totalDiscounted = this.discounted;
        for (int i = 0; i < shared.length; i++) {
            product = productList.get(i);
            befDiscounted = product.getDiscounted();
            product.addDiscounted(sub(product.getCurrentPrice(), shared[i]));
            totalDiscounted = add(totalDiscounted,
                    sub(product.getDiscounted(), befDiscounted));
//            product.updateCurrentPrice();
        }

        this.discounted = totalDiscounted;
    }

    public void updateCurrentPrice() {
        this.currentPrice = sub(
                mul(this.originalPrice, this.nums), this.discounted);

        this.getPacageProdList().forEach(product -> product.updateCurrentPrice());
    }

    public void addPacageProd(Product p) {
        this.productList.add(p);
    }

    public List<Product> getPacageProdList() {
        return this.productList;
    }

    public int getExpireFlag() {
        return expireFlag;
    }

    public void setExpireFlag(int expireFlag) {
        this.expireFlag = expireFlag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Package aPackage = (Package) o;

        return packageId == aPackage.packageId;

    }

    @Override
    public int hashCode() {
        return (int) (packageId ^ (packageId >>> 32));
    }

    @Override
    public String toString() {
        return "Package{" +
                "packageId=" + packageId +
                ", nums=" + nums +
                ", originalPrice=" + originalPrice +
                ", currentPrice=" + currentPrice +
                ", discounted=" + discounted +
                '}';
    }
}
