package com.onek.discount.aaa.ccb.service;

import com.onek.discount.aaa.ccb.entity.IDiscount;
import com.onek.discount.aaa.ccb.entity.Ladoff;

public abstract class BaseDiscountContent implements IDiscountContent {
    protected int getStragy(long bRule) {
        char index = String.valueOf(bRule).charAt(4);

        return Character.digit(index, 10);
    }



}
