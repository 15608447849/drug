package com.onek.calculate.rule;

import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.entity.Gift;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.util.DiscountUtil;

import java.util.List;

public final class RuleParser {
    private static final IRuleHandler[] RULE_HANDLER = {
        new SubRuleHandler(),
        new GiftRuleHandler(),
    };

    private long offerCode;

    public RuleParser(long offerCode) {
        this.offerCode = offerCode;
    }

    private int getCalStrategy() {
        return Character.digit(String.valueOf(this.offerCode).charAt(4), 10);
    }

    private int getRuleHandler() {
        return Character.digit(String.valueOf(this.offerCode).charAt(1), 10);
    }

    private int getHandler() {
        return Character.digit(String.valueOf(this.offerCode).charAt(2), 10);
    }

    public void parser(IDiscount discount, Ladoff ladoff) {
        if (ladoff == null) {
            return ;
        }

        IRuleHandler handler = RULE_HANDLER[getRuleHandler() - 1];

        ValueAndTimes vt = getValueAndTimes(discount, ladoff);

        switch (getHandler()) {
            case 1:
                handler.subHandler(discount, vt.value, vt.times);
                break;
            case 2:
                handler.shippingHandler(discount, vt.value, vt.times);
                break;
            case 3:
                handler.percentHandler(discount, vt.value, vt.times);
                break;
            case 4:
                handler.giftHandler(discount, ladoff.getGiftList(), vt.times);
                break;
            default:
                break;
        }
    }

    private ValueAndTimes getValueAndTimes(IDiscount discount, Ladoff ladoff) {
        int times = 0;
        switch (getCalStrategy()) {
            case 1:
                times = meiMan(
                        ladoff.getLadnum(), DiscountUtil.getNumTotal(discount.getProductList()),
                        ladoff.getLadamt(), DiscountUtil.getCurrentPriceTotal(discount.getProductList()));
                break;
            case 2:
                times = jianMian(ladoff.getLadnum(), DiscountUtil.getNumTotal(discount.getProductList()),
                        ladoff.getLadamt(), DiscountUtil.getCurrentPriceTotal(discount.getProductList()));
                break;
            default:
                break;
        }

        return new ValueAndTimes(ladoff.getOffer(), times);
    }

    private int meiMan(int ladNum, int nums, double ladAmt, double total) {
        int times = 0;

        if (ladNum > 0 && ladAmt > 0) {
            times = Math.min(nums / ladNum, (int) (total / ladAmt));
        } else if (ladNum > 0) {
            times = nums / ladNum;
        } else if (ladAmt > 0) {
            times = (int) (total / ladAmt);
        }

        return times;
    }

    private int jianMian(int ladNum, int nums, double ladAmt, double total) {
        return 1;
    }

    private class ValueAndTimes {
        private double value;
        private int times;

        public ValueAndTimes(double value, int times) {
            this.value = value;
            this.times = times;
        }
    }
}

interface IRuleHandler {
    void subHandler(IDiscount discount, double value, int times);
    void percentHandler(IDiscount discount, double value, int times);
    void shippingHandler(IDiscount discount, double value, int times);
    void giftHandler(IDiscount discount, List<Gift> gifts, int times);
}
