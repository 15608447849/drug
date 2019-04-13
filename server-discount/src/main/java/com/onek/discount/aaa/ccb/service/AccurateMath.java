package com.onek.discount.aaa.ccb.service;

import java.math.BigDecimal;

public abstract class AccurateMath {
    protected double add(double a, double b) {
        BigDecimal ba = BigDecimal.valueOf(a);
        BigDecimal bb = BigDecimal.valueOf(b);

        return ba.add(bb).setScale(2).doubleValue();
    }

    protected double sub(double a, double b) {
        BigDecimal ba = BigDecimal.valueOf(a);
        BigDecimal bb = BigDecimal.valueOf(b);

        return ba.subtract(bb).setScale(2).doubleValue();
    }

    protected double mul(double a, double b) {
        return BigDecimal.valueOf(a * b).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    protected double div(double a, double b) {

        return BigDecimal.valueOf(a / b).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


}
