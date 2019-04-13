package com.onek.discount.calculate.service;

public abstract class BaseDiscountContent implements IDiscountContent {
    protected int getStragy(long bRule) {
        char index = String.valueOf(bRule).charAt(4);

        return Character.digit(index, 10);
    }



}
