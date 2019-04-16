package com.onek.discount.calculate.service.calculate;

import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.IProduct;
import com.onek.discount.calculate.entity.Ladoff;
import com.onek.discount.calculate.rule.RuleParser;
import com.onek.discount.calculate.service.calculate.ICalculateService;
import util.MathUtil;

import java.util.List;

public abstract class BaseDiscountCalculateService implements ICalculateService {
    @Override
    public void calculate(List<? extends IDiscount> discountList) {
        for (IDiscount iDiscount : discountList) {
            discountHandler(iDiscount);
        }

        for (IDiscount iDiscount : discountList) {
            iDiscount.updateAllPrices();
        }
    }

   /* protected boolean doSP(IDiscount discount, Ladoff ladoff) {
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
    }*/

    protected void discountHandler(IDiscount discount) {
        long actNo = discount.getDiscountNo();

        Ladoff[] ladoff = getLadoffs(actNo);

        if (ladoff == null || ladoff.length == 0) {
            return;
        }

        /*if (doSP(discount, ladoff[0])) {
            return ;
        }*/

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

        new RuleParser(discount.getBRule()).parser(discount, currentLadoff);
    }

    protected final Ladoff getLadoffable(Ladoff[] ladoffs, double price, int nums) {
        double ladAmt;
        int ladNum;
        boolean able;

        for (Ladoff ladoff : ladoffs) {
            ladAmt = ladoff.getLadamt();
            ladNum = ladoff.getLadnum();
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

    protected abstract Ladoff[] getLadoffs(long actCode);
}

