package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.IDiscount;

import java.util.List;

public interface ICalculateService {
    void calculate(List<IDiscount> discountList);
}
