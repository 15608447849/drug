package com.onek.calculate.rule;

import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.Gift;
import com.onek.calculate.entity.IDiscount;

import java.util.List;

public class GiftRuleHandler implements IRuleHandler {
    @Override
    public void subHandler(IDiscount discount, double value, int times) {
        if (discount instanceof Activity) {
            discount.addGift(
                    Gift.getSubCoupon(
                            value, times, discount.getDiscountNo(),
                            ((Activity) discount).getCurrLadoff()));
        } else {
            discount.addGift(Gift.getSubCoupon(value, times, discount.getDiscountNo(), null));
        }
    }

    @Override
    public void percentHandler(IDiscount discount, double value, int times) {
        discount.addGift(Gift.getPercentCoupon(value, times, discount.getDiscountNo()));
    }

    @Override
    public void shippingHandler(IDiscount discount, double value, int times) {
        discount.addGift(Gift.getFreeShipping(times, discount.getDiscountNo()));
    }

    @Override
    public void giftHandler(IDiscount discount, List<Gift> gifts, int times) {
        for (Gift gift : gifts) {
            gift.setNums(times);
        }

        discount.addGifts(gifts);
    }
}
