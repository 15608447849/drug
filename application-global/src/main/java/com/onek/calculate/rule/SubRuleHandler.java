package com.onek.calculate.rule;

import com.onek.calculate.entity.Gift;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.util.DiscountUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.List;

public class SubRuleHandler implements IRuleHandler {
    @Override
    public void subHandler(IDiscount discount, double value, int times) {
        discount.setDiscounted(MathUtil
                .exactMul(value, times)
                .doubleValue());
    }

    @Override
    public void percentHandler(IDiscount discount, double value, int times) {
        double p = BigDecimal.ONE
                    .subtract(BigDecimal.valueOf(value).divide(BigDecimal.TEN))
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
                    .doubleValue();

        discount.setDiscounted(
                MathUtil.exactMul(p, DiscountUtil.getCurrentPriceTotal(discount.getProductList()))
                        .doubleValue());
    }

    @Override
    public void shippingHandler(IDiscount discount, double value, int times) {
        discount.setFreeShipping(true);
    }

    @Override
    public void giftHandler(IDiscount discount, List<Gift> gifts, int times) {}

}
