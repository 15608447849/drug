package com.onek.discount.calculate.service.calculate;

import com.onek.discount.calculate.entity.IDiscount;

import java.util.List;

public interface ICalculateService {
    void calculate(List<? extends IDiscount> discountList);
}
