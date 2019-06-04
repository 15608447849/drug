package util;

import java.util.Arrays;
import java.util.List;

public class ArrayUtil {
    public static boolean isEmpty(Object[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * 加在队首。
     * @param arr
     * @param added
     * @param <T>
     * @return
     */
    public static <T> T[] unshift(T[] arr, T... added) {
        return concat(added, arr);
    }

    /**
     * 加在队尾。
     * @param arr
     * @param added
     * @param <T>
     * @return
     */
    public static <T> T[] push(T[] arr, T... added) {
        return concat(arr, added);
    }

    public static <T> T[] concat(T[] front, T[] tail) {
        if (isEmpty(front)) {
            return tail;
        }

        if (isEmpty(tail)) {
            return front;
        }

        T[] result = Arrays.copyOf(front, front.length + tail.length);

        for (int i = 0; i < tail.length; i++) {
            result[i + front.length] = tail[i];
        }

        return result;

    }

    public static double[] concat(double[] front, double[] tail) {
        if (front == null || front.length == 0) {
            return tail;
        }

        if (tail == null || tail.length == 0) {
            return front;
        }

        double[] result = Arrays.copyOf(front, front.length + tail.length);

        for (int i = 0; i < tail.length; i++) {
            result[i + front.length] = tail[i];
        }

        return result;
    }

    /**
     * 移除队尾。
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> T[] pop(T[] arr) {
        if (isEmpty(arr)) {
            return arr;
        }

        if (arr.length == 1) {
            return (T[]) new Object[0];
        }

        return Arrays.copyOfRange(arr, 0, arr.length - 1);
    }

    /**
     * 移除队首。
     * @param arr
     * @param <T>
     * @return
     */
    public static <T> T[] shift(T[] arr) {
        if (isEmpty(arr)) {
            return arr;
        }

        if (arr.length == 1) {
            return (T[]) new Object[0];
        }

        return Arrays.copyOfRange(arr, 1, arr.length);
    }

}
