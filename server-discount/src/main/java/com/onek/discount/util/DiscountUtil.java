package com.onek.discount.util;

import util.MathUtil;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Random;

public class DiscountUtil {

    public static double[] shareDiscount(double[] prices, double discount) {
        if (discount <= 0) {
            return prices;
        }

        double[] result = new double[prices.length];
        double total = 0.0;
        double realDiscountTotal = 0.0;
        double turlyDiscountTotal;
        double proportion;
        double deviation;

        for (double price : prices) {
            total = MathUtil.exactAdd(total, price).doubleValue();
        }

        if (total <= discount) {
            Arrays.fill(result, 0.0);
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
}
