package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.Gift;
import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.IProduct;
import com.onek.discount.aaa.ccb.entity.Ladoff;

import java.util.List;

public class GiftDiscountContent extends BaseDiscountContent {

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

            List<IProduct> prods = discount.getProductList();

            for (IProduct prod : prods) {
                prod.addGift(Gift.getSubCoupon(ladoff.getOfferValue(), times));
            }
        } else if (s == 2) {
            List<IProduct> prods = discount.getProductList();

            for (IProduct prod : prods) {
                prod.addGift(Gift.getSubCoupon(ladoff.getOfferValue(), 1));
            }
        }
    }

    @Override
    public void percent(IDiscount discount, Ladoff ladoff) {
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

            List<IProduct> prods = discount.getProductList();

            for (IProduct prod : prods) {
                prod.addGift(Gift.getPercentCoupon(ladoff.getOfferValue(), times));
            }
        } else if (s == 2) {
            List<IProduct> prods = discount.getProductList();

            for (IProduct prod : prods) {
                prod.addGift(Gift.getPercentCoupon(ladoff.getOfferValue(), 1));
            }
        }
    }

    @Override
    public void shipping(IDiscount discount, Ladoff ladoff) {
        List<IProduct> prods = discount.getProductList();

        for (IProduct prod : prods) {
            prod.addGift(Gift.getFreeShipping(1));
        }
    }

    @Override
    public void gift(IDiscount discount, Ladoff ladoff) {
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

            List<IProduct> prods = discount.getProductList();
            List<Gift> gifts = ladoff.getGiftList();
            for (Gift gift : gifts) {
                gift.setNums(times);
            }

            for (IProduct prod : prods) {
                prod.addGifts(gifts);
            }
        } else if (s == 2) {
            List<IProduct> prods = discount.getProductList();

            for (IProduct prod : prods) {
                prod.addGifts(ladoff.getGiftList());
            }
        }
    }
}
