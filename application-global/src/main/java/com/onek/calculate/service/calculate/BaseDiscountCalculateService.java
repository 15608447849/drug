package com.onek.calculate.service.calculate;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.rule.RuleParser;
import com.onek.calculate.util.DiscountUtil;
import util.ArrayUtil;

import java.util.List;

public abstract class BaseDiscountCalculateService implements ICalculateService {
    protected IDiscount currDiscount = null;

    @Override
    public void calculate(List<? extends IDiscount> discountList) {
        for (IDiscount iDiscount : discountList) {
            currDiscount = iDiscount;

            if (currDiscount.getBRule() == 1133
             || currDiscount.getBRule() == 1113) {
                // 不计算团购和秒杀
                continue;
            }

            discountHandler();
        }

        DiscountUtil.updateAllPrices(DiscountUtil.getProds(discountList));
    }

    protected void discountHandler() {
        long actNo = currDiscount.getDiscountNo();

        Ladoff[] ladoff = getLadoffs(actNo);

        if (ArrayUtil.isEmpty(ladoff)) {
            return;
        }

        double priceTotal = DiscountUtil.getCurrentPriceTotal(currDiscount.getProductList());
        int numsTotal = DiscountUtil.getNumTotal(currDiscount.getProductList());

        Ladoff currentLadoff = getLadoffable(ladoff, priceTotal, numsTotal);

        if (currentLadoff == null) {
            return;
        }

        new RuleParser(currentLadoff.getOffercode()).parser(currDiscount, currentLadoff);
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

    /* 按照最高满足持续降序排列 */
    protected abstract Ladoff[] getLadoffs(long actCode);
}

