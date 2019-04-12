package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.Ladoff;
import util.MathUtil;

import java.math.BigDecimal;

public class SubDiscountContent extends BaseDiscountContent {
    @Override
    public void sub(IDiscount discount, Ladoff ladoff) {
        int s = getStragy(ladoff.getOfferCode());

        if (s == 1) {
            int ladNum = ladoff.getLadNum();
            double ladAmt = ladoff.getLadAmt();
            int times = 0;

            if (ladNum > 0 && ladAmt > 0) {
                times = Math.min(discount.getNumTotal() / ladNum, (int) (discount.getCurrentPriceTotal() / ladAmt));
            } else if (ladNum > 0) {
                times = discount.getNumTotal() / ladNum;
            } else if (ladAmt > 0) {
                times = (int) (discount.getCurrentPriceTotal() / ladAmt);
            }

            discount.addDiscounted(MathUtil
                    .exactMul(ladoff.getOfferValue(), times)
                    .doubleValue());
        } else if (s == 2) {
            discount.addDiscounted(ladoff.getOfferValue());
        }
    }

    @Override
    public void percent(IDiscount discount, Ladoff ladoff) {
        int s = getStragy(ladoff.getOfferCode());

        if (s == 2) {
            double p = BigDecimal
                        .valueOf(1 - (ladoff.getOfferValue() / 100))
                        .setScale(2, BigDecimal.ROUND_HALF_UP)
                        .doubleValue();

            discount.addDiscounted(
                    MathUtil.exactMul(p, discount.getCurrentPriceTotal())
                    .doubleValue());
        }
    }

    @Override
    public void shipping(IDiscount discount, Ladoff ladoff) {
        discount.setFreeShipping(true);
    }

    @Override
    public void gift(IDiscount discount, Ladoff ladoff) {
    }
}
