package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.Gift;
import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.IProduct;
import com.onek.discount.calculate.entity.Ladoff;

import java.util.List;

public class GiftDiscountContent extends BaseDiscountContent {

    @Override
    public void sub(IDiscount discount, Ladoff ladoff) {
        int s = getStragy(ladoff.getOffercode());

        if (s == 1) {
            int ladNum = ladoff.getLadnum();
            double ladAmt = ladoff.getLadamt();
            int times = 0;

            if (ladNum > 0 && ladAmt > 0) {
                times = Math.min(discount.getNumTotal() / ladNum, (int) (discount.getCurrentPriceTotal() / ladAmt));
            } else if (ladNum > 0) {
                times = discount.getNumTotal() / ladNum;
            } else if (ladAmt > 0) {
                times = (int) (discount.getCurrentPriceTotal() / ladAmt);
            }

            discount.addGift(Gift.getSubCoupon(ladoff.getOffer(), times));
        } else if (s == 2) {
            discount.addGift(Gift.getSubCoupon(ladoff.getOffer(), 1));
        }
    }

    @Override
    public void percent(IDiscount discount, Ladoff ladoff) {
        int s = getStragy(ladoff.getOffercode());

        if (s == 1) {
            int ladNum = ladoff.getLadnum();
            double ladAmt = ladoff.getLadamt();
            int times = 0;

            if (ladNum > 0 && ladAmt > 0) {
                times = Math.min(discount.getNumTotal() / ladNum, (int) (discount.getCurrentPriceTotal() / ladAmt));
            } else if (ladNum > 0) {
                times = discount.getNumTotal() / ladNum;
            } else if (ladAmt > 0) {
                times = (int) (discount.getCurrentPriceTotal() / ladAmt);
            }

            discount.addGift(Gift.getPercentCoupon(ladoff.getOffer(), times));
        } else if (s == 2) {
            discount.addGift(Gift.getPercentCoupon(ladoff.getOffer(), 1));
        }
    }

    @Override
    public void shipping(IDiscount discount, Ladoff ladoff) {
        discount.addGift(Gift.getFreeShipping(1));
    }

    @Override
    public void gift(IDiscount discount, Ladoff ladoff) {
        int s = getStragy(ladoff.getOffercode());

        if (s == 1) {
            int ladNum = ladoff.getLadnum();
            double ladAmt = ladoff.getLadamt();
            int times = 0;

            if (ladNum > 0 && ladAmt > 0) {
                times = Math.min(discount.getNumTotal() / ladNum, (int) (discount.getCurrentPriceTotal() / ladAmt));
            } else if (ladNum > 0) {
                times = discount.getNumTotal() / ladNum;
            } else if (ladAmt > 0) {
                times = (int) (discount.getCurrentPriceTotal() / ladAmt);
            }

            List<Gift> gifts = ladoff.getGiftList();
            for (Gift gift : gifts) {
                gift.setNums(times);
            }

            discount.addGifts(gifts);
        } else if (s == 2) {
            discount.addGifts(ladoff.getGiftList());
        }
    }
}
