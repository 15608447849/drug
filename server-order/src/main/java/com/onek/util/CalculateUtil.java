package com.onek.util;

import com.alibaba.fastjson.JSONArray;
import com.onek.calculate.ActivityCalculateService;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.CouponFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.*;
import com.onek.calculate.service.calculate.CouponCalculateService;
import com.onek.calculate.util.DiscountUtil;
import util.ArrayUtil;
import util.MathUtil;

import java.math.BigDecimal;
import java.util.*;

public class CalculateUtil {
    private static Comparator<Integer> DESC_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    };

    public static double getProdMinPrice(long sku, double price) {
        if (sku <= 0 || price <= 0) {
            return .0;
        }

        List<Product> products = new ArrayList<>(1);
        Product p = new Product();
        p.setSku(sku);
        p.autoSetCurrentPrice(price, 1);
        products.add(p);

        List<IDiscount> discounts =
                new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new PriorityFilter() }
                ).getCurrentActivities(products);

        if (!discounts.isEmpty()) {
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
                        // 获取最后一个
                        curr = ls[ls.length - 1];
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
                                    discounted += price * (1 - value);
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

        return price;
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

        return (value * price) / amt;
    }

    public static List<IDiscount> getDiscount(int compid, List<? extends IProduct> products) {
        if (compid <= 0) {
            return Collections.EMPTY_LIST;
        }

        List<IDiscount> discounts
                = new ActivityFilterService(
                new ActivitiesFilter[] {
                        new CycleFilter(),
                        new QualFilter(compid),
                        new PriorityFilter(), })
                .getCurrentActivities(products);

        return discounts;
    }

    public static DiscountResult calculate(int compid,
                                           List<? extends IProduct> products,
                                           long couponUnqid) {
        if (compid <= 0) {
            return new DiscountResult(Collections.EMPTY_LIST, 0.0, products);
        }

        List<IDiscount> activityList =
                new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(),
                        }).getCurrentActivities(products);

        if (!activityList.isEmpty()) {
            TreeMap<Integer, List<IDiscount>> pri_act = new TreeMap<>(DESC_COMPARATOR);

            int priority;
            List<IDiscount> temp;
            for (IDiscount discount : activityList) {
                priority = discount.getPriority();

                temp = pri_act.get(priority);

                if (temp == null) {
                    temp = new ArrayList<>();
                    pri_act.put(priority, temp);
                }

                temp.add(discount);
            }


            for (List<IDiscount> discounts : pri_act.values()) {
                new ActivityCalculateService().calculate(discounts);
            }
        }

        double couponValue = 0.0;

        if (couponUnqid > 0) {
            List<IDiscount> couponFilter =
                    new CouponFilterService(
                            couponUnqid,
                            DiscountUtil.isExcoupon(activityList),
                            compid).getCurrentActivities(products);

            if (!couponFilter.isEmpty()) {
                Couent couent = (Couent) couponFilter.get(0);

                new CouponCalculateService(
                        JSONArray.parseArray(
                                couent.getLadder(), Ladoff.class)
                                .toArray(new Ladoff[0]))
                        .calculate(couponFilter);

                couponValue = couent.getDiscounted();

                activityList.add(couent);
            }
        }

        return new DiscountResult(activityList, couponValue, products);
    }
}
