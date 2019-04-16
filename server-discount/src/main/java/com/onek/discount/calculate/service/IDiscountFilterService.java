package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.IProduct;

import java.util.List;

public interface IDiscountFilterService {
    List<IDiscount> getCurrentActivities(List<? extends IProduct> products);
}
