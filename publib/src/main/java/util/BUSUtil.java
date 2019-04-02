package util;

import java.util.Arrays;

public class BUSUtil {
    public static int getBreak(int[] points, int start, int end) {
        if (points == null) {
            throw new IllegalArgumentException("the points is null.");
        }

        if (start >= end) {
            throw new IllegalArgumentException("start can't bigger than end!");
        }

        if (points.length == 0) {
            return start;
        }

        Arrays.sort(points);

        if (points[0] < start || points[points.length - 1] > end) {
            throw new IllegalArgumentException("point can't bigger than end or smaller than start!");
        }

        if (points[0] > start) {
            return start;
        }

        int before = points[0];

        for (int i = 1; i < points.length; i++) {
            //等于不视为断点。
            if (before != points[i]) {
                if (points[i] > (before + 1)) {
                    break;
                }
            }

            before = points[i];
        }

        return before + 1;
    }
}
