package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;

/**
 * 类型过滤器。用以过滤团购类型。
 */

public class TypeFilter extends BaseFilter {
    protected boolean isFilter(IDiscount activity) {
        long bRule = activity.getBRule();

        return bRule == 1133;
    }
}