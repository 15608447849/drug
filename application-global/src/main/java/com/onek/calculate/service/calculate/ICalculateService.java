package com.onek.calculate.service.calculate;

import com.onek.calculate.entity.IDiscount;

import java.util.List;

public interface ICalculateService {
    void calculate(List<? extends IDiscount> discountList);
}
