package com.onek.util;

import com.alibaba.fastjson.JSONArray;
import com.onek.calculate.ActivityCalculateService;
import com.onek.calculate.ActivityFilterService;
import com.onek.calculate.CouponFilterService;
import com.onek.calculate.entity.*;
import com.onek.calculate.filter.*;
import com.onek.calculate.service.calculate.CouponCalculateService;
import com.onek.calculate.util.DiscountUtil;
import org.hyrdpf.ds.AppConfig;

import java.util.*;

public class CalculateUtil {
    private static Comparator<Integer> DESC_COMPARATOR = new Comparator<Integer>() {
        @Override
        public int compare(Integer o1, Integer o2) {
            return o2 - o1;
        }
    };

    public static List<Ladoff[]> getLadoff(int compid, List<? extends IProduct> products) {
        List<Ladoff[]> result = new ArrayList<>();

        List<IDiscount> discounts = getDiscount(compid, products);

        for (IDiscount discount : discounts) {
            result.add(new ActivityCalculateService().getLadoffs(discount.getDiscountNo()));
        }

        return result;
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
                        new PriorityFilter(),
                        new StoreFilter(),})
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
                                new StoreFilter(),
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

    /*static {
        *//**初始化LOG4J2日志环境*//*
        AppConfig.initLogger("log4j2.xml");
        *//**初始化应用程序环境，如数据源等*//*
        AppConfig.initialize();
    }

    public static void main(String[] args) {
        List<Product> pl = new ArrayList<>();
        Product p = new Product();
        p.setSku(11000000001201L);
        p.setNums(10);
        pl.add(p);
        calculate(536862726, pl, 0);
    }*/
}
