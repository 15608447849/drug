package com.onek.calculate.entity;

import java.util.Collection;

public interface IProduct {
    long getSKU();
    int getNums();
//    void addGift(Gift gift);
//    void addGifts(List<Gift> gifts);
//    void addActivity(long activity);
    double getOriginalPrice();
    double getCurrentPrice();
    void setCurrentPrice(double currentPrice);
    void setOriginalPrice(double originalPrice);
    void addDiscounted(double discounted);
    double getDiscounted();
    void updateCurrentPrice();
    void addActivityCode(String actCode);
    Collection<String> getActivityCodes();
}
