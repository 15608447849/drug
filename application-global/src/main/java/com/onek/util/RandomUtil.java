package com.onek.util;

import com.onek.calculate.util.DiscountUtil;

import java.util.Random;

/**
 * 随机生成工具类。
 * @author Helena Rubinstein
 *
 */

public class RandomUtil {
    private static final Random RANDOM = new Random();
    
    /**
     * 获取一个随机的字母、数字混合的字符串。
     * @param len
     * @return
     */
    
    public static String getRandomLetterMixNumber(int len) {
        if (len < 1) {
            throw new IllegalArgumentException("len is less than 1...");
        }
        
        char[] results = new char[len];
        
        for (; len > 0; len--) {
            results[len - 1] =
                    RANDOM.nextBoolean() 
                        ? nextSingleNumber() 
                        : RANDOM.nextBoolean() 
                            ? nextLowerLetter() 
                            : nextUpperLetter() ; 
        }
        
        return String.valueOf(results);
    }

    /**
     * 获取一个随机的数字的字符串。
     * @param len
     * @return
     */

    public static String getRandomNumber(int len) {
        if (len < 1) {
            throw new IllegalArgumentException("len is less than 1...");
        }

        char[] results = new char[len];

        for (; len > 0; len--) {
            results[len - 1] = nextSingleNumber();
        }

        return String.valueOf(results);
    }


    /**
     * 获取一个随机的大写字母
     * @return
     */
    
    public static char nextUpperLetter() {
        return (char) nextInt(65, 91);
    }
    
    /**
     * 获取一个随机的小写字母
     * @return
     */
    
    public static char nextLowerLetter() {
        return (char) nextInt(97, 123);
    }
    
    /**
     * 获取一个0-9的随机数字
     * @return
     */
    
    public static char nextSingleNumber() {
        return (char) nextInt(48, 57);
    }
    
    public static int nextInt() {
        return nextInt(0, Integer.MAX_VALUE);
    }
    
    
    /**
     * 生成一个int数，范围为[start, end)
     * @param start
     * @param end
     * @return
     */
    
    public static int nextInt(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start is bigger than end!");
        }
        
        if (start == end) {
            return end;
        }
        
        return start + RANDOM.nextInt(end - start);
    }

    
}
