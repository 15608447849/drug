package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;

import java.util.List;

public interface ActivitiesFilter {
    void doFilter(List<? extends IDiscount> activities, IProduct product);
}
