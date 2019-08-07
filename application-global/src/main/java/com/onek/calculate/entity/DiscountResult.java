package com.onek.calculate.entity;

import util.MathUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class DiscountResult {
    private boolean freeShipping; // 是否免邮
    private double totalCurrentPrice; // 总活动价格
    private double totalDiscount; // 总优惠
    private boolean exCoupon; //是否排斥优惠券
    private int totalNums;
    private double couponValue;
    private double rebeatValue;
    private List<Gift> giftList; // 总赠品
    private List<IDiscount> activityList; // 活动列表
    private boolean pkgExpired;

    public DiscountResult(List<IDiscount> activityList,
                          double couponValue,
                          List<? extends IProduct> products) {
        this.activityList = activityList;
        this.couponValue = couponValue;

        this.totalNums = prodsNum(products);
        this.totalCurrentPrice = prodsCurrentTotal(products);
        this.giftList = new ArrayList<>();

        for (IProduct product : products) {
            if (product instanceof Package) {
                if (((Package) product).getExpireFlag() < 0) {
                    pkgExpired = true;
                    break;
                }
            }
        }

        for (IDiscount discount : activityList) {
            this.freeShipping = this.freeShipping || discount.getFreeShipping() ;
            this.exCoupon = this.exCoupon || discount.getExCoupon();
            this.totalDiscount =
                    MathUtil.exactAdd(discount.getDiscounted(), this.totalDiscount)
                            .doubleValue();


            if (discount.getGiftList() != null && !discount.getGiftList().isEmpty()) {
                giftList.addAll(discount.getGiftList());

                if (isRebeatAct(discount.getBRule())) {
                    this.rebeatValue =
                            MathUtil.exactAdd(discount.getGiftList().get(0).getGiftValue(), this.rebeatValue)
                                    .doubleValue();
                }
            }
        }

        this.totalDiscount = MathUtil.decimal(2, this.totalDiscount);
        this.rebeatValue = MathUtil.decimal(2, this.rebeatValue);
    }

    private boolean isRebeatAct(long brule) {
        String bruleStr = brule + "";

        return bruleStr.charAt(0) == '1'
              && bruleStr.charAt(1) == '2'
              && bruleStr.charAt(2) == '1';
    }

    private int prodsNum(List<? extends IProduct> products) {
        int result = 0;

        for (IProduct product : products) {
            if (product instanceof Package) {
                result += ((Package) product).getTotalNums();
            } else {
                result += product.getNums();
            }
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

    public List<Gift> getGiftList() {
        return giftList;
    }

    public boolean isPkgExpired() {
        return pkgExpired;
    }

    public double getRebeatValue() {
        return rebeatValue;
    }
}
