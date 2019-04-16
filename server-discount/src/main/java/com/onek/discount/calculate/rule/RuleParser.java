package com.onek.discount.calculate.rule;

import com.onek.discount.calculate.entity.Gift;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.Ladoff;

import java.util.List;

public class RuleParser {
    private static final IRuleHandler[] RULE_HANDLER = {
        new SubRuleHandler(),
        new GiftRuleHandler(),
    };

    private long brule;

    public RuleParser(long brule) {
        this.brule = brule;
    }

    protected int getCalStrategy() {
        return Character.digit(String.valueOf(this.brule).charAt(4), 10);
    }

    protected int getRuleHandler() {
        return Character.digit(String.valueOf(this.brule).charAt(1), 10);
    }

    protected int getHandler() {
        return Character.digit(String.valueOf(this.brule).charAt(2), 10);
    }

    public void parser(IDiscount discount, Ladoff ladoff) {
        IRuleHandler handler = RULE_HANDLER[getRuleHandler() - 1];

        ValueAndTimes vt = getValueAndTimes(discount, ladoff);

        switch (getHandler()) {
            case 1 :
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
        int times;
        switch (getCalStrategy()) {
            case 1 :
                times = meiMan(
                        ladoff.getLadnum(), discount.getNumTotal(),
                        ladoff.getLadamt(), discount.getCurrentPriceTotal());
                break;
            case 2 :
            default:
                times = 1;
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
