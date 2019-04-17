package com.onek.calculate.entity;

public class Gift {
    private interface TYPE {
        int FREESHIPPING = 0;
        int SUB = 1;
        int PERCENT = 2;
        int GIFT = 3;
    }

    private long id;
    private String giftName;
    private double giftValue;
    private int nums = 1;
    private int type = TYPE.GIFT;

    public static Gift getSubCoupon(double giftValue, int nums) {
        Gift gift = new Gift();
        gift.giftValue = giftValue;
        gift.nums = nums;
        gift.giftName = "满减券";
        gift.type = TYPE.SUB;

        return gift;
    }

    public static Gift getFreeShipping(int nums) {
        Gift gift = new Gift();
        gift.giftName = "免邮券";
        gift.nums = nums;
        gift.type = TYPE.FREESHIPPING;

        return gift;
    }

    public static Gift getPercentCoupon(double giftValue, int nums) {
        Gift gift = new Gift();
        gift.giftName = "折扣券";
        gift.giftValue = giftValue;
        gift.nums = nums;
        gift.type = TYPE.PERCENT;

        return gift;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
}
