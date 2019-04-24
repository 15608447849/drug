package com.onek.util;

import com.alibaba.fastjson.JSONArray;
import com.onek.calculate.ActivityCalculateService;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.CouponFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.*;
import com.onek.calculate.service.calculate.CouponCalculateService;
import com.onek.calculate.util.DiscountUtil;
import util.StringUtils;

import java.util.*;

public class CalculateUtil {
    private static Comparator<Integer> DESC_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    };


    public static List<IDiscount> getDiscount(int compid, List<? extends IProduct> products) {
        if (compid <= 0) {
            return Collections.EMPTY_LIST;
        }

        List<IDiscount> discounts
                = new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new QualFilter(compid),
                                new PriorityFilter(),})
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
                                new TypeFilter(),
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
