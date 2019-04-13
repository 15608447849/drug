package com.onek.discount.aaa.ccb.filter;

import com.onek.discount.aaa.ccb.entity.IDiscount;

import java.util.List;

public interface ActivitiesFilter {
    void doFilter(List<IDiscount> activities);
}
