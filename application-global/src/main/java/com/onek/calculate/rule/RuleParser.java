package com.onek.calculate.rule;

import com.onek.calculate.entity.Gift;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.util.DiscountUtil;
import util.MathUtil;

import java.math.RoundingMode;
import java.util.List;

public final class RuleParser {
    private static final IRuleHandler[] RULE_HANDLER = {
        new SubRuleHandler(),
        new GiftRuleHandler(),
    };

    private static final ICalStrategy[] CALTIMES_STRATEGY = {
        new Meiman(),
        new Manjian(),
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
        double currentPrice = DiscountUtil.getCurrentPriceTotal(discount.getProductList());
        int totalNum = DiscountUtil.getNumTotal(discount.getProductList());

        int times = CALTIMES_STRATEGY[getCalStrategy() - 1]
                .getTimes(
                        ladoff.getLadnum(),
                        totalNum,
                        ladoff.getLadamt(),
                        currentPrice);

        double value = ladoff.getOffer();

        if (ladoff.isPercentage()) {
            value = MathUtil.exactMul(
                    currentPrice, ladoff.getOffer()).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        return new ValueAndTimes(value, times);
    }

    private static class ValueAndTimes {
        private double value;
        private int times;

        public ValueAndTimes(double value, int times) {
            this.value = value;
            this.times = times;
        }
    }

    private interface ICalStrategy {
        int getTimes(int ladNum, int nums, double ladAmt, double total);
    }


    private static class Manjian implements ICalStrategy {

        @Override
        public int getTimes(int ladNum, int nums, double ladAmt, double total) {
            return 1;
        }
    }

    private static class Meiman implements ICalStrategy {

        @Override
        public int getTimes(int ladNum, int nums, double ladAmt, double total) {
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
    }
}

interface IRuleHandler {
    void subHandler(IDiscount discount, double value, int times);
    void percentHandler(IDiscount discount, double value, int times);
    void shippingHandler(IDiscount discount, double value, int times);
    void giftHandler(IDiscount discount, List<Gift> gifts, int times);
}
