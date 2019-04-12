package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.Ladoff;

public interface IDiscountContent {
    void sub(IDiscount discount, Ladoff ladoff);
    void percent(IDiscount discount, Ladoff ladoff);
    void shipping(IDiscount discount, Ladoff ladoff);
    void gift(IDiscount discount, Ladoff ladoff);
}
