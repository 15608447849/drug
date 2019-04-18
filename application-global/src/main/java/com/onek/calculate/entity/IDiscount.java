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

    default double[] getEachCurrent() {
        List<IProduct> prodList = getProductList();
        double[] results = new double[prodList.size()];
        IProduct product;
        for (int i = 0; i < results.length; i++) {
            product = prodList.get(i);

            results[i] = product.getCurrentPrice();
        }

        return results;
    }

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

    void addGift(Gift gift);
    void addGifts(List<Gift> gifts);

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
