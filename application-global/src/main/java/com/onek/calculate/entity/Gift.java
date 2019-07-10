package com.onek.calculate.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import util.StringUtils;

public class Gift {
    private interface TYPE {
        int FREESHIPPING = 0;
        int SUB = 1;
        int PERCENT = 2;
        int GIFT = 3;
    }

    private long id;
    private String giftName;
    private String giftDesc;
    private double giftValue;
    private int giftNum = 1;
    private int nums = 1;
    private int type = TYPE.GIFT;
    private long activityCode;

    public static Gift getSubCoupon(double giftValue, int nums, long activityCode) {
        Gift gift = new Gift();
        gift.activityCode = activityCode;
        gift.giftValue = giftValue;
        gift.nums = nums;
        gift.giftName = "现金券";
        gift.type = TYPE.SUB;

        return gift;
    }

    public static Gift getFreeShipping(int nums, long activityCode) {
        Gift gift = new Gift();
        gift.activityCode = activityCode;
        gift.giftName = "包邮券";
        gift.nums = nums;
        gift.type = TYPE.FREESHIPPING;

        return gift;
    }

    public static Gift getPercentCoupon(double giftValue, int nums, long activityCode) {
        Gift gift = new Gift();
        gift.activityCode = activityCode;
        gift.giftName = "折扣券";
        gift.giftValue = giftValue;
        gift.nums = nums;
        gift.type = TYPE.PERCENT;

        return gift;
    }

    public String getId() {
        try {
            if (!StringUtils.isEmpty(this.giftDesc)) {
                JSONObject desc = JSON.parseObject(this.giftDesc);
                String erpsku = desc.getString("erpsku");
                return erpsku == null ? "" : erpsku;
            }
        } catch (Exception e) { e.printStackTrace(); }

        return "";
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTurlyId() {
        return id;
    }

    public String getGiftName() {
        return giftName;
    }

    public void setGiftName(String giftName) {
        this.giftName = giftName;
    }

    public double getGiftValue() {
        return giftValue;
    }

    public void setGiftValue(double giftValue) {
        this.giftValue = giftValue;
    }

    public int getNums() {
        return nums;
    }

    public void setNums(int nums) {
        this.nums = nums;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getGiftDesc() {
        return giftDesc;
    }

    public void setGiftDesc(String giftDesc) {
        this.giftDesc = giftDesc;
    }

    public long getActivityCode() {
        return activityCode;
    }

    public void setActivityCode(long activityCode) {
        this.activityCode = activityCode;
    }

    public int getGiftNum() {
        return giftNum;
    }

    public void setGiftNum(int giftNum) {
        this.giftNum = giftNum;
    }

    public int getTotalNums() {
        return this.giftNum * this.nums;
    }
}
