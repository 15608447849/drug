package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.IProduct;
import com.onek.discount.calculate.filter.ActivitiesFilter;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseDiscountFilterService implements IDiscountFilterService {
    protected ActivitiesFilter[] discountFilters;

    public BaseDiscountFilterService(ActivitiesFilter[] discountFilters) {
        this.discountFilters = discountFilters;
    }

    protected void doFilter(List<IDiscount> activityList) {
        if (discountFilters == null) {
            return;
        }

        for (ActivitiesFilter discountFilter : discountFilters) {
            discountFilter.doFilter(activityList);
        }
    }

    public List<IDiscount> getCurrentActivities(List<IProduct> products) {
        List<IDiscount> result = new ArrayList<>();
        List<IDiscount> temp;
        int index;
        for (IProduct product : products) {
            temp = getCurrentDiscounts(product.getSKU());

            for (IDiscount activity : temp) {
                index = result.indexOf(activity);

                if (index == -1) {
                    activity.addProduct(product);
                    result.add(activity);
                } else {
                    result.get(index).addProduct(product);
                }

            }
        }

        return result;
    }

    protected final int checkSKU(long sku) {
        int length = String.valueOf(sku).length();

        switch (length) {
            case 14 :
                return 0;
            default :
                return -1;
        }
    }

    protected final String getProductCode(long sku) {
        if (checkSKU(sku) < 0) {
            return null;
        }

        return String.valueOf(sku).substring(1, 7);
    }

    public abstract List<IDiscount> getCurrentDiscounts(long sku);
}
