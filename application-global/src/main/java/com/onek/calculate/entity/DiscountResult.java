package com.onek.calculate.entity;

import com.onek.calculate.util.DiscountUtil;
import util.MathUtil;

import java.util.List;

public class DiscountResult {
    private boolean freeShipping; // 是否免邮
    private double totalCurrentPrice; // 总活动价格
    private double totalDiscount; // 总优惠
    private boolean exCoupon; //是否排斥优惠券
    private int totalNums;
    private double couponValue;
    private List<IDiscount> activityList; // 活动列表

    public DiscountResult(List<IDiscount> activityList, double couponValue) {
        this.activityList = activityList;

        for (IDiscount discount : activityList) {
            this.freeShipping = this.freeShipping || discount.getFreeShipping() ;
            this.exCoupon = this.exCoupon || discount.getExCoupon();
            this.totalDiscount =
                    MathUtil.exactAdd(discount.getDiscounted(), this.totalDiscount)
                            .doubleValue();
        }

        this.couponValue = couponValue;
        this.totalNums = DiscountUtil.getTotalNums(activityList);
        this.totalCurrentPrice = DiscountUtil.getTotalCurrentPrice(activityList);
    }

    public boolean isFreeShipping() {
        return freeShipping;
    }

    public void setFreeShipping(boolean freeShipping) {
        this.freeShipping = freeShipping;
    }

    public double getTotalCurrentPrice() {
        return totalCurrentPrice;
    }

    public void setTotalCurrentPrice(double totalCurrentPrice) {
        this.totalCurrentPrice = totalCurrentPrice;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public boolean isExCoupon() {
        return exCoupon;
    }

    public void setExCoupon(boolean exCoupon) {
        this.exCoupon = exCoupon;
    }

    public List<IDiscount> getActivityList() {
        return activityList;
    }

    public void setActivityList(List<IDiscount> activityList) {
        this.activityList = activityList;
    }

    public int getTotalNums() {
        return totalNums;
    }

    public void setTotalNums(int totalNums) {
        this.totalNums = totalNums;
    }

    public double getCouponValue() {
        return couponValue;
    }

    public void setCouponValue(double couponValue) {
        this.couponValue = couponValue;
    }
}
