package com.onek.discount.aaa.ccb.entity;

import java.util.List;

public interface IProduct {
    long getSKU();
    int getNums();
    void addGift(Gift gift);
    void addGifts(List<Gift> gifts);
    void addActivity(long activity);
    double getOriginalPrice();
    double getCurrentPrice();
    void setCurrentPrice(double currentPrice);
    void addSharePrice(double sharePrice);
    void updateCurrentPrice();
}
