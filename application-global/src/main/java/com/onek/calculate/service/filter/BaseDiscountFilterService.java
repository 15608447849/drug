package com.onek.calculate.service.filter;

import com.onek.calculate.filter.ActivitiesFilter;
import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;

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

    public List<IDiscount> getCurrentActivities(List<? extends IProduct> products) {
        List<IDiscount> result = new ArrayList<>();
        List<IDiscount> temp;
        int index;
        for (IProduct product : products) {
            temp = getCurrentDiscounts(product.getSKU());

            // 不参与活动的商品不加入。
            doFilter(temp);

            for (IDiscount activity : temp) {
                index = result.indexOf(activity);

                if (index == -1) {
                    activity.addProduct(product);
                    result.add(activity);
                } else {
                    result.get(index).setLimits(
                            product.getSKU(), activity.getLimits(product.getSKU()));
                    result.get(index).addProduct(product);
                }

            }
        }

        return result;
    }

    private final int checkSKU(long sku) {
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
            throw new IllegalArgumentException("SKU is illegal, " + sku);
        }

        return String.valueOf(sku).substring(1, 7);
    }

    protected abstract List<IDiscount> getCurrentDiscounts(long sku);
}
