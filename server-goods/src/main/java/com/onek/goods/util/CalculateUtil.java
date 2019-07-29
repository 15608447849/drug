package com.onek.goods.util;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Ladoff;
import com.onek.goods.calculate.ActivityCalculateService;
import util.ArrayUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class CalculateUtil {
    private static Comparator<Integer> DESC_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    };

    public static double getProdMinPrice(long sku, double price, List<IDiscount> discounts) {
        if (sku <= 0 || price <= 0) {
            return .0;
        }

        if (discounts != null && !discounts.isEmpty()) {
            TreeMap<Integer, List<IDiscount>> pri_act = new TreeMap<>(DESC_COMPARATOR);

            int priority;
            List<IDiscount> temp;
            for (IDiscount discount : discounts) {
                priority = discount.getPriority();

                temp = pri_act.get(priority);

                if (temp == null) {
                    temp = new ArrayList<>();
                    pri_act.put(priority, temp);
                }

                temp.add(discount);
            }

            for (List<IDiscount> dss : pri_act.values()) {
                Ladoff[] ls;
                Ladoff curr;
                double value, amt, discounted = .0;
                int num;
                for (IDiscount discount : dss) {
                    if (discount.getBRule() == 1113) {
                        price = discount.getActionPrice(sku);
                        break;
                    } else if (discount.getBRule() == 1133) {
                        break;
                    }

                    ls = new ActivityCalculateService().getLadoffs(discount.getDiscountNo());

                    if (!ArrayUtil.isEmpty(ls)) {
                        // 获取第一个
                        curr = ls[0];
                        if (Character.digit(String.valueOf(curr.getOffercode()).charAt(1), 10) == 1) {
                            value = curr.getOffer();
                            amt = curr.getLadamt();
                            num = curr.getLadnum();

                            switch (Character.digit(String.valueOf(curr.getOffercode()).charAt(2), 10)) {
                                case 1:
                                    double amtDiscount = getLatamtDiscount(value, amt, price);
                                    double numDiscount = getLatNumPrice(value, num);

                                    discounted += Math.max(amtDiscount, numDiscount);
                                    break;
                                case 3:
                                    discounted += price * (1 - (value / 10));
                                    break;
                            }
                        }
                    }
                }

                price = MathUtil
                        .exactSub(price, discounted)
                        .setScale(2, BigDecimal.ROUND_HALF_UP)
                        .doubleValue();

            }

        }

        return Math.max(price, 0);
    }

    private static double getLatNumPrice(double value, int num) {
        if (num <= 0) {
            return .0;
        }

        return value / num;
    }

    private static double getLatamtDiscount(double value, double amt, double price) {
        if (amt <= 0) {
            return .0;
        }

        return price >= amt ? value : (value * price) / amt;
    }
}
