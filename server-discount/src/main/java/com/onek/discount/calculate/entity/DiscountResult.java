package com.onek.discount.calculate.entity;

import java.util.List;

public class DiscountResult {
    private boolean freeShipping; // 是否免邮
    private double totalDiscountPrice; // 总活动价格
    private double totalDiscount; // 总优惠
    private boolean exCoupon; //是否排斥优惠券
    private List<IDiscount> activityList; // 活动列表
}
