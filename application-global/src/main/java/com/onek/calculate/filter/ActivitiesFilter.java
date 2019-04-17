package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;

import java.util.List;

public interface ActivitiesFilter {
    void doFilter(List<? extends IDiscount> activities);
}
