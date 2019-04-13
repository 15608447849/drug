package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.IProduct;
import com.onek.discount.aaa.ccb.entity.Ladoff;
import com.onek.discount.util.DiscountUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.List;

public abstract class BaseDiscountCalculateService implements ICalculateService {
    private static final long KILL = 1113;

    private static final IDiscountContent[] I_DISCOUNT_CONTENTS = {
        new SubDiscountContent(),
        new GiftDiscountContent(),
    };

    @Override
    public void calculate(List<IDiscount> discountList) {
        for (IDiscount iDiscount : discountList) {
            discountHandler(iDiscount);
        }

        for (IDiscount iDiscount : discountList) {
            iDiscount.updateAllPrices();
        }
    }

    protected boolean doSP(IDiscount discount, Ladoff ladoff) {
        List<IProduct> prods = discount.getProductList();
        if (KILL == discount.getBRule()) {
            for (IProduct product : prods) {
                double dd = product.getCurrentPrice() - (ladoff.getOfferValue() * product.getNums());
                product.addSharePrice(dd);
                discount.setDiscounted(discount.getDiscounted() + dd);
            }

            return true;
        }

        return false;
    }

    protected void discountHandler(IDiscount discount) {
        long bRule = discount.getBRule();

        Ladoff[] ladoff = getLadoffs(bRule);

        if (ladoff == null || ladoff.length == 0) {
            return;
        }

        if (doSP(discount, ladoff[0])) {
            return ;
        }

        List<IProduct> prods = discount.getProductList();
        double priceTotal = 0.0;
        int numsTotal = 0;

        for (IProduct prod : prods) {
            priceTotal = MathUtil.exactAdd(
                    prod.getCurrentPrice(), priceTotal).doubleValue();
            numsTotal = MathUtil.exactAdd(
                    prod.getNums(), numsTotal).intValue();
        }

        Ladoff currentLadoff = getLadoffable(ladoff, priceTotal, numsTotal);

        if (currentLadoff == null) {
            return;
        }

        int index = Character.digit(String.valueOf(bRule).charAt(1), 10) - 1;

        IDiscountContent contents = I_DISCOUNT_CONTENTS[index];

        int way = Character.digit(String.valueOf(bRule).charAt(2), 10);

        switch (way) {
            case 1 :
                contents.sub(discount, currentLadoff);
                break;
            case 2:
                contents.shipping(discount, currentLadoff);
                break;
            case 3:
                contents.percent(discount, currentLadoff);
                break;
            case 4:
                contents.gift(discount, currentLadoff);
                break;
        }
    }

    protected final Ladoff getLadoffable(Ladoff[] ladoffs, double price, int nums) {
        double ladAmt;
        int ladNum;
        boolean able;

        for (Ladoff ladoff : ladoffs) {
            ladAmt = ladoff.getLadAmt();
            ladNum = ladoff.getLadNum();
            able = true;
            // 全为0则直接拿value
            if (ladAmt > 0 && ladNum > 0) {
                able = price >= ladAmt && nums >= ladNum;
            } else if (ladAmt > 0) {
                able = price >= ladAmt;
            } else if (ladNum > 0) {
                able = price >= ladNum;
            }

            if (able) {
                return ladoff;
            }

        }

        return null;
    }

    protected abstract Ladoff[] getLadoffs(long brule);


}

