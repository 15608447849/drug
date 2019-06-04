package com.onek.report.core;

import java.math.BigDecimal;

public interface IDoubleCal {
    default double addDouble(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
    }

    default double subDouble(double a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b)).doubleValue();
    }

    default double mulDouble(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)).doubleValue();
    }

    default double divDouble(double a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b)).doubleValue();
    }
}
