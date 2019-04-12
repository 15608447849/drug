package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;

import java.util.List;

public interface ICalculateService {
    void calculate(List<IDiscount> discountList);
}
