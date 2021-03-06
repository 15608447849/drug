package com.onek.calculate.service.filter;

import com.onek.calculate.entity.*;
import com.onek.calculate.entity.Package;
import com.onek.calculate.filter.ActivitiesFilter;
import util.MathUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
            if (checkSKU(product.getSKU()) < 0) {
                continue;
            }

            temp = getCurrentDiscounts(product);
            doFilter(temp, product);

            for (IDiscount activity : temp) {
                index = result.indexOf(activity);
                product.addActivityCode(String.valueOf(activity.getDiscountNo()));

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

                if (activity.getBRule() == 1114 && product instanceof Package) {
                    Package p = (Package) product;

                    List<Product> prodList = p.getPacageProdList();
                    BigDecimal totalDiscounted = BigDecimal.ZERO;
                    double discounted;
                    for (Product product1 : prodList) {
                        discounted = MathUtil.exactSub(
                                product1.getCurrentPrice(),
                                activity.getActionPrice(product1.getSKU())
                                        * product1.getNums()).setScale(2, RoundingMode.HALF_UP).doubleValue();

                        totalDiscounted = totalDiscounted.add(BigDecimal.valueOf(discounted));
                        product1.addDiscounted(discounted);
                    }
                    discounted = totalDiscounted.setScale(2, RoundingMode.HALF_UP).doubleValue();
                    ((Activity) activity).setDiscountedOnly(discounted);
                    p.setDiscounted(discounted);
                    p.updateCurrentPrice();
                    p.setExpireFlag(0);
                }

            }

        }

        return result;
    }

    private final int checkSKU(long sku) {
        return sku == 0 ? -1 : 0;
//        int length = String.valueOf(sku).length();
//
//        switch (length) {
//            case 14 :
//                return 0;
//            default :
//                return -1;
//        }
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
