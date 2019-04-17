package com.onek.calculate.util;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.*;

public class DiscountUtil {
    public static double[] shareDiscount(double[] prices, double discount) {
        if (discount <= 0) {
            throw new IllegalArgumentException("折扣价小于0");
        }

        double[] result = new double[prices.length];
        double total = Arrays.stream(prices).sum();
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

        proportion = MathUtil.exactSub(1, discount / total).doubleValue();

        for (int i = 0; i < prices.length; i++) {
            result[i] = BigDecimal.valueOf(proportion * prices[i])
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

            realDiscountTotal = MathUtil.exactAdd(result[i], realDiscountTotal).doubleValue();
        }

        deviation = MathUtil.exactSub(turlyDiscountTotal, realDiscountTotal).doubleValue();

        if (deviation != 0) {
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

    public static double getTotalCurrentPrice(List<? extends IDiscount> discounts) {
        BigDecimal bd = BigDecimal.ZERO;
        Set<IProduct> productSet = new HashSet<>();

        for (IDiscount discount : discounts) {
            productSet.addAll(discount.getProductList());
        }

        for (IProduct iProduct : productSet) {
            bd = bd.add(BigDecimal.valueOf(iProduct.getCurrentPrice()));
        }

        return bd.setScale(2).doubleValue();
    }

    public static int getTotalNums(List<? extends IDiscount> discounts) {
        int nums = 0;
        Set<IProduct> productSet = new HashSet<>();

        for (IDiscount discount : discounts) {
            productSet.addAll(discount.getProductList());
        }

        for (IProduct iProduct : productSet) {
            nums += iProduct.getNums();
        }

        return nums;
    }


}
