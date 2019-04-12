package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.IProduct;

import java.util.List;

public interface IDiscountFilterService {
    List<IDiscount> getCurrentActivities(List<IProduct> products);
}
