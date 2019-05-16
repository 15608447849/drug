package com.onek.calculate.service.filter;

import com.onek.calculate.entity.Product;
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

    public BaseDiscountFilterService() {}

    protected void doFilter(List<IDiscount> activityList, IProduct product) {
        if (discountFilters == null || product == null) {
            return;
        }

        for (ActivitiesFilter discountFilter : discountFilters) {
            discountFilter.doFilter(activityList, product);
        }
    }

    public List<IDiscount> getCurrentActivities(List<? extends IProduct> products) {
        List<IDiscount> result = new ArrayList<>();
        List<IDiscount> temp;
        int index;
        IDiscount i;
        for (IProduct product : products) {
            temp = getCurrentDiscounts(product);

            doFilter(temp, product);

            for (IDiscount activity : temp) {
                index = result.indexOf(activity);

                if (index == -1) {
                    activity.addProduct(product);
                    result.add(activity);
                } else {
                    i = result.get(index);
                    i.setLimits(
                            product.getSKU(), activity.getLimits(product.getSKU()));
                    i.setActionPrice(
                            product.getSKU(),
                            activity.getActionPrice(product.getSKU()));
                    i.addProduct(product);
                }

                if (activity.getBRule() == 1113 && product instanceof Product) {
                    // 秒杀特殊处理
                    Product p = (Product) product;
                    p.autoSetCurrentPrice(activity.getActionPrice(p.getSKU()), p.getNums());
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

    protected final String[] getProductCode(long sku) {
        if (checkSKU(sku) < 0) {
            throw new IllegalArgumentException("SKU is illegal, " + sku);
        }

        String classNo = String.valueOf(sku).substring(1, 7);

        return new String[] {
                classNo.substring(0, 2),
                classNo.substring(0, 4),
                classNo.substring(0, 6) };
    }

    protected abstract List<IDiscount> getCurrentDiscounts(IProduct product);
}
