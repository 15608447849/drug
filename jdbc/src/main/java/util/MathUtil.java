package util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

        return a1.divide(b1, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 精准相除。
     * @return
     */
    public static BigDecimal exactDiv(double a, long b) {
        BigDecimal a1 = BigDecimal.valueOf(a);
        BigDecimal b1 = BigDecimal.valueOf(b);

        return a1.divide(b1, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 将一个数分隔成2的幂次之和
     * @param num
     * @return
     */

    public static List<Long> spiltNumToBit(long num) {
        if (num <= 0) {
            return Collections.emptyList();
        }

        List<Long> result = new ArrayList<>();

        int power = 0;
        while (num > 0) {
            if ((num & 1) > 0) {
                result.add(1L << power);
            }

            num >>= 1;
            power++;
        }

        return result;
    }

    public long getMax(long... nums) {
        if (ArrayUtil.isEmpty(nums)) {
            throw new IllegalArgumentException("ur compared parms is empty!");
        }

        if (nums.length == 1) {
            return nums[0];
        }

        if (nums.length == 2) {
            return Math.max(nums[1], nums[0]);
        }

        return Arrays.stream(nums).max().getAsLong();
    }

    public double getMax(double... nums) {
        if (ArrayUtil.isEmpty(nums)) {
            throw new IllegalArgumentException("ur compared parms is empty!");
        }

        if (nums.length == 1) {
            return nums[0];
        }

        if (nums.length == 2) {
            return Math.max(nums[1], nums[0]);
        }

        return Arrays.stream(nums).max().getAsDouble();
    }

    public long getMin(long... nums) {
        if (ArrayUtil.isEmpty(nums)) {
            throw new IllegalArgumentException("ur compared parms is empty!");
        }

        if (nums.length == 1) {
            return nums[0];
        }

        if (nums.length == 2) {
            return Math.min(nums[1], nums[0]);
        }

        return Arrays.stream(nums).min().getAsLong();
    }

    public double getMin(double... nums) {
        if (ArrayUtil.isEmpty(nums)) {
            throw new IllegalArgumentException("ur compared parms is empty!");
        }

        if (nums.length == 1) {
            return nums[0];
        }

        if (nums.length == 2) {
            return Math.min(nums[1], nums[0]);
        }

        return Arrays.stream(nums).min().getAsDouble();
    }

}
