package com.onek.discount.calculate.rule;

import com.onek.discount.calculate.entity.Gift;
import com.onek.discount.calculate.entity.IDiscount;

import java.util.List;

public class GiftRuleHandler implements IRuleHandler {
    @Override
    public void subHandler(IDiscount discount, double value, int times) {
        discount.addGift(Gift.getSubCoupon(value, times));
    }

    @Override
    public void percentHandler(IDiscount discount, double value, int times) {
        discount.addGift(Gift.getPercentCoupon(value, times));
    }

    @Override
    public void shippingHandler(IDiscount discount, double value, int times) {
        discount.addGift(Gift.getFreeShipping(times));
    }

    @Override
    public void giftHandler(IDiscount discount, List<Gift> gifts, int times) {
        for (Gift gift : gifts) {
            gift.setNums(times);
        }

        discount.addGifts(gifts);
    }
}
