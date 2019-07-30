package com.onek.goods.service;

import com.onek.calculate.entity.Activity;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.Product;
import com.onek.calculate.filter.*;
import com.onek.goods.calculate.ActivityCalculateService;
import com.onek.goods.calculate.ActivityFilterService;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description
 * @time 2019/7/22 14:59
 **/
public class PromLoadOffService {

    public List<String> getLadOffBySku(long skuList){
        List<Product> productList = new ArrayList<>();
        Product product = new Product();
        product.setSku(skuList);
        productList.add(product);
//        for (long sku : skuList) {
//        }
        List<String> ladOffList = new ArrayList<>();
        List<IDiscount> activityList =
                new ActivityFilterService(
                        new ActivitiesFilter[] {
                                new CycleFilter(),
                                new PriorityFilter(),
                                new StoreFilter(),
                        }).getCurrentActivities(productList);
        new ActivityCalculateService().calculate(activityList);
        activityList.sort((o1, o2) -> o2.getPriority() - o1.getPriority());
        for (IDiscount iDiscount : activityList) {
            Activity activity = (Activity)iDiscount;
            if (!activity.isGlobalActivity()) {
                if (!activity.getLadoffDescs().isEmpty()) {
                    String ladOff = activity.getLadoffDescs().get(activity.getLadoffDescs().size() - 1);
                    ladOffList.add(ladOff);
                }
            }
        }
        return ladOffList;
    }
}
