package com.onek.report.core;

import util.MathUtil;

import java.math.BigInteger;

public interface ILongCal {
    default long addLong(long a, long b) {
        return BigInteger.valueOf(a).add(BigInteger.valueOf(b)).longValue();
    }

    default long subLong(long a, long b) {
        return BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)).longValue();
    }

    default long mulLong(long a, long b) {
        return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).longValue();
    }

    default long divLong(long a, long b) {
        return BigInteger.valueOf(a).divide(BigInteger.valueOf(b)).longValue();
    }
}
