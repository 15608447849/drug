package com.onek.calculate.service.calculate;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.calculate.rule.RuleParser;
import com.onek.calculate.util.DiscountUtil;
import util.ArrayUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.List;

public abstract class BaseDiscountCalculateService implements ICalculateService {
    private IDiscount currDiscount = null;

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
        double ladAmt, gapAmt;
        int ladNum, gapNum;
        boolean able;

        for (Ladoff ladoff : ladoffs) {
            ladAmt = ladoff.getLadamt();
            ladNum = ladoff.getLadnum();
            gapAmt = MathUtil.exactSub(price, ladAmt)
                             .setScale(2, BigDecimal.ROUND_HALF_UP)
                             .doubleValue();

            gapNum = nums - ladNum;

            able = true;
            // 全为0则直接拿value
            if (ladAmt > 0 && ladNum > 0) {
                able = gapAmt >= 0 && gapNum >= 0;
            } else if (ladAmt > 0) {
                able = gapAmt >= 0;
            } else if (ladNum > 0) {
                able = gapNum >= 0;
            }

            if (able) {
                currDiscount.setCurrLadoff(ladoff);
                return ladoff;
            }

            currDiscount.setNextGapAmt(gapAmt);
            currDiscount.setNextGapNum(gapNum);
            currDiscount.setNextLadoff(ladoff);
        }

        return null;
    }

    /* 按照最高满足持续降序排列 */
    protected abstract Ladoff[] getLadoffs(long actCode);

}

