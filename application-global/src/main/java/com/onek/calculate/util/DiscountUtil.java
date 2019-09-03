package com.onek.calculate.util;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import util.MathUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class DiscountUtil {
    public static double[] shareDiscount(double[] prices, double discount) {
        if (discount < 0) {
            throw new IllegalArgumentException("折扣价小于0");
        }

        if (discount == 0) {
            return prices;
        }

        double[] result = new double[prices.length];
        BigDecimal totalExtra = BigDecimal.ZERO;

        for (double price : prices) {
            totalExtra = totalExtra.add(BigDecimal.valueOf(price));
        }

        double total = totalExtra.setScale(2, RoundingMode.HALF_UP).doubleValue();
        double realDiscountTotal = .0;
        double turlyDiscountTotal;
        double proportion;
        double deviation;

        if (total <= discount) {
            Arrays.fill(result, 0);
            return result;
        }

        turlyDiscountTotal = MathUtil.exactSub(total, discount).doubleValue();

        if (prices.length == 1) {
            Arrays.fill(result, turlyDiscountTotal);
            return result;
        }

        if (discount == .01) {
            for (int i = 0; i < result.length; i++) {
                if (result[i] > .01) {
                    result[i] = MathUtil.exactAdd(result[i], -.01).doubleValue();
                    return result;
                }
            }

            for (int i = 0; i < result.length; i++) {
                if (result[i] >= .01) {
                    result[i] = MathUtil.exactAdd(result[i], -.01).doubleValue();
                    return result;
                }
            }
        }

//        proportion = MathUtil.exactSub(1, discount / total).doubleValue();
        proportion = 1 - discount / total;

        for (int i = 0; i < prices.length; i++) {
            result[i] = BigDecimal.valueOf(proportion * prices[i])
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            realDiscountTotal = MathUtil.exactAdd(result[i], realDiscountTotal).doubleValue();
        }

        deviation = MathUtil.exactSub(turlyDiscountTotal, realDiscountTotal).doubleValue();

        if (deviation != 0) {
            for (int i = 0; i < result.length; i++) {
                if (result[i] > Math.abs(deviation)) {
                    result[i] = MathUtil.exactAdd(result[i], deviation).doubleValue();
                    return result;
                }
            }

            for (int i = 0; i < result.length; i++) {
                if (result[i] >= Math.abs(deviation)) {
                    result[i] = MathUtil.exactAdd(result[i], deviation).doubleValue();
                    return result;
                }
            }

            int randomIndex = new Random().nextInt(prices.length);

            result[randomIndex] = MathUtil.exactAdd(result[randomIndex], deviation).doubleValue();
        }

        return result;
    }

    public static boolean isExcoupon(List<? extends IDiscount> discounts) {
        boolean result = false;

        for (IDiscount discount : discounts) {
            result = result || discount.getExCoupon();
        }

        return result;
    }

    public static double[] getEachCurrent(List<? extends IProduct> prodList) {
        double[] results = new double[prodList.size()];
        IProduct product;
        for (int i = 0; i < results.length; i++) {
            product = prodList.get(i);

            results[i] = product.getCurrentPrice();
        }

        return results;
    }

    public static List<IProduct> getProds(List<? extends IDiscount> discounts) {
        Set<IProduct> resultSet = new HashSet<>();

        for (IDiscount discount : discounts) {
            resultSet.addAll(discount.getProductList());
        }

        return new ArrayList<>(resultSet);
    }

    public static void updateAllPrices(List<? extends IProduct> prodList) {
        for (IProduct product: prodList) {
            product.updateCurrentPrice();
        }
    }

    public static double getCurrentPriceTotal(List<? extends IProduct> prodList) {
        BigDecimal result = BigDecimal.ZERO;

        for (IProduct product: prodList) {
            result = result.add(BigDecimal.valueOf(product.getCurrentPrice()));
        }

        return result.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static int getNumTotal(List<? extends IProduct> prodList) {
        int result = 0;

        for (IProduct product: prodList) {
            result += product.getNums();
        }

        return result;
    }

}
