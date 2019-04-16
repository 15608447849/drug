package com.onek.discount.calculate.filter;

import com.onek.discount.calculate.entity.IDiscount;

import java.util.List;

public interface ActivitiesFilter {
    void doFilter(List<? extends IDiscount> activities);
}
