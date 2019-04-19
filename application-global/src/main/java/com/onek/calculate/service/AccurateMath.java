package com.onek.calculate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class AccurateMath {
    protected double add(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    protected double sub(double a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    protected double mul(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    protected double div(double a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }


}
