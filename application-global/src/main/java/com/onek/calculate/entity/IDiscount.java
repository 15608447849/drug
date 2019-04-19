package com.onek.calculate.entity;

import com.onek.calculate.util.DiscountUtil;
import util.MathUtil;

import java.math.BigDecimal;
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
    void setExCoupon(boolean exCoupon);
    boolean getExCoupon();

    void setLimits(long sku, int limits);
    int getLimits(long sku);

    void setActionPrice(long sku, double price);
    double getActionPrice(long sku);

    String getStartTime();
    String getEndTime();

    void addGift(Gift gift);
    void addGifts(List<Gift> gifts);

}
