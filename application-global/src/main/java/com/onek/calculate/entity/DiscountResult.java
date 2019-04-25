package com.onek.calculate.entity;

import util.MathUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class DiscountResult {
    private boolean freeShipping; // 是否免邮
    private double totalCurrentPrice; // 总活动价格
    private double totalDiscount; // 总优惠
    private boolean exCoupon; //是否排斥优惠券
    private int totalNums;
    private double couponValue;
    private List<IDiscount> activityList; // 活动列表

    public DiscountResult(List<IDiscount> activityList,
                          double couponValue,
                          List<? extends IProduct> products) {
        this.activityList = activityList;
        this.couponValue = couponValue;

        this.totalNums = prodsNum(products);
        this.totalCurrentPrice = prodsCurrentTotal(products);

        for (IDiscount discount : activityList) {
            this.freeShipping = this.freeShipping || discount.getFreeShipping() ;
            this.exCoupon = this.exCoupon || discount.getExCoupon();
            this.totalDiscount =
                    MathUtil.exactAdd(discount.getDiscounted(), this.totalDiscount)
                            .doubleValue();
        }

        this.totalDiscount = MathUtil.decimal(2, this.totalDiscount);
    }

    private int prodsNum(List<? extends IProduct> products) {
        int result = 0;

        for (IProduct product : products) {
            result += product.getNums();
        }

        return result;
    }

    private double prodsCurrentTotal(List<? extends IProduct> products) {
        BigDecimal result = BigDecimal.ZERO;

        for (IProduct product : products) {
            result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
        }

        return result.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }



    public boolean isFreeShipping() {
        return freeShipping;
    }

    public double getTotalCurrentPrice() {
        return totalCurrentPrice;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public boolean isExCoupon() {
        return exCoupon;
    }

    public List<IDiscount> getActivityList() {
        return activityList;
    }

    public int getTotalNums() {
        return totalNums;
    }

    public double getCouponValue() {
        return couponValue;
    }
}
