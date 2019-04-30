package com.onek.calculate.service.calculate;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.rule.RuleParser;
import com.onek.calculate.util.DiscountUtil;

import java.util.List;

public abstract class BaseDiscountCalculateService implements ICalculateService {
    @Override
    public void calculate(List<? extends IDiscount> discountList) {
        for (IDiscount iDiscount : discountList) {
            if (iDiscount.getBRule() == 1133
             || iDiscount.getBRule() == 1113) {
                // 不计算团购和秒杀
                continue;
            }

            discountHandler(iDiscount);
        }

        DiscountUtil.updateAllPrices(DiscountUtil.getProds(discountList));
    }

    protected void discountHandler(IDiscount discount) {
        long actNo = discount.getDiscountNo();

        Ladoff[] ladoff = getLadoffs(actNo);

        if (ladoff == null || ladoff.length == 0) {
            return;
        }

//        List<IProduct> prods = discount.getProductList();

        double priceTotal = DiscountUtil.getCurrentPriceTotal(discount.getProductList());
        int numsTotal = DiscountUtil.getNumTotal(discount.getProductList());

        /*for (IProduct prod : prods) {
            priceTotal = MathUtil.exactAdd(
                    prod.getCurrentPrice(), priceTotal).doubleValue();
            numsTotal = MathUtil.exactAdd(
                    prod.getNums(), numsTotal).intValue();
        }*/

        Ladoff currentLadoff = getLadoffable(ladoff, priceTotal, numsTotal);

        if (currentLadoff == null) {
            return;
        }

        new RuleParser(currentLadoff.getOffercode()).parser(discount, currentLadoff);
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

