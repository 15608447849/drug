package util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * 与数学运算有关的工具类。
 * @author Helena Rubinstein
 *
 */

public class MathUtil {
    
    /**
     * 判定某数是否在指定的范围内。
     * @param min
     * @param tar
     * @param max
     * @return tar ∈ [min. max]返回true
     */
    
    public static boolean isBetween(double min, double tar, double max) {
        return min <= tar && tar <= max;
    }
    
    public static long bitAnd(long a1, long a2) {
        return a1 & a2;
    }

    public static int bitAnd(int a1, int a2) {
        return a1 & a2;
    }

    public static long bitOr(long a1, long a2) {
        return a1 | a2;
    }

    public static int bitOr(int a1, int a2) {
        return a1 | a2;
    }

    /**
     * 判定某数是否在指定的范围内。
     * @param min
     * @param tar
     * @param max
     * @return tar ∈ [min. max]返回true
     */
    
    public static boolean isBetween(long min, long tar, long max) {
        return min <= tar && tar <= max;
    }
    
    
    
    /**
     * 比较俩个数大小。
     * @param d1
     * @param d2
     * @return d1 > d2 -> 1; d1 == d2 -> 0; d1 < d2 -> -1;
     */
    
    public static int compare(double d1, double d2) {
        return d1 > d2 ? 1 : (d1 == d2 ? 0 : -1);
    }
    
    
    /**
     * d1 / d2 并向上取整。
     * @param d1 被除数
     * @param d2 除数
     * @return
     */
    
    public static int ceilDiv(int d1, int d2) {
        return (int) Math.ceil((double) d1 / (double) d2);
    }

    /**
     * 保留m位小数
     * @param m 位数
     * @param d1 数值
     */
    public static double decimal(int m,double d1){
        BigDecimal bg = new BigDecimal(d1);
        double f1 = bg.setScale(m, BigDecimal.ROUND_HALF_UP).doubleValue();
        return f1;
    }
    
    
    /**
     * 精准相加。
     * @return
     */
    public static BigDecimal exactAdd(double a, double b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);
        
        return a1.add(b1);
    }
    
    
    /**
     * 精准相减。
     * @return
     */
    public static BigDecimal exactSub(double a, double b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);
        
        return a1.subtract(b1);
    }
    

    
    /**
     * 精准相乘。
     * @return
     */
    public static BigDecimal exactMul(double a, double b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);
        
        return a1.multiply(b1);
    }

    /**
     * 精准相乘。
     * @return
     */
    public static BigDecimal exactMul(double a, long b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);

        return a1.multiply(b1);
    }


    /**
     * 精准相除。
     * @return
     */
    public static BigInteger exactDiv(long a, long b) {
        BigInteger a1 = BigInteger.valueOf(a);
        BigInteger b1 = BigInteger.valueOf(b);
        
        return a1.divide(b1);
    }
    
    /**
     * 精准相除。
     * @return
     */
    public static BigDecimal exactDiv(double a, double b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);

        return a1.divide(b1);
    }

    /**
     * 精准相除。
     * @return
     */
    public static BigDecimal exactDiv(double a, long b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);

        return a1.divide(b1);
    }
}
