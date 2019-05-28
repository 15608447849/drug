package com.onek.calculate.entity;

import java.util.List;

public interface IDiscount {
    long getDiscountNo();
    long getBRule();

    int getPriority();
    void setDiscounted(double discount);
    double getDiscounted();

    void addProduct(IProduct product);
    List<IProduct> getProductList();
    void setFreeShipping(boolean free);
    boolean getFreeShipping();
    boolean getExCoupon();

    void setLimits(long sku, int limits);
    int getLimits(long sku);

    void setActionPrice(long sku, double price);
    double getActionPrice(long sku);

    String getStartTime();
    String getEndTime();

    void addGift(Gift gift);
    void addGifts(List<Gift> gifts);
    List<Gift> getGiftList();

    default void setCurrLadoff(Ladoff currLadoff) {}
    default void setNextLadoff(Ladoff nextLadoff) {}

    default void setNextGapAmt(double nextGapAmt) {}
    default void setNextGapNum(int nextGapNum) {}
}
