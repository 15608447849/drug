package com.onek.discount.calculate.service;

import com.onek.discount.calculate.entity.IDiscount;
import com.onek.discount.calculate.entity.Ladoff;

public interface IDiscountContent {
    void sub(IDiscount discount, Ladoff ladoff);
    void percent(IDiscount discount, Ladoff ladoff);
    void shipping(IDiscount discount, Ladoff ladoff);
    void gift(IDiscount discount, Ladoff ladoff);
}
