package com.onek.calculate.service.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;

import java.util.List;

public interface IDiscountFilterService {
    List<IDiscount> getCurrentActivities(List<? extends IProduct> products);
}
