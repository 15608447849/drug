package com.onek.discount.calculate.entity;

import java.math.BigDecimal;
import java.util.List;

public interface IDiscount {
    long getActNo();
    int getActtype();
    long getActcycle();
    int getIncpriority();
    int getQualcode();
    int getQualvalue();
    long getBRule();

    int getPriority();
    void addDiscounted(double discount);
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

    String getStartTime();
    String getEndTime();

    default void updateAllPrices() {
        for (IProduct product: getProductList()) {
            product.updateCurrentPrice();
        }
    }

    default double getCurrentPriceTotal() {
        BigDecimal result = BigDecimal.ZERO;

        for (IProduct product: getProductList()) {
            result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
        }

        return result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    default int getNumTotal() {
        int result = 0;

        for (IProduct product: getProductList()) {
            result += product.getNums();
        }

        return result;
    }
}
