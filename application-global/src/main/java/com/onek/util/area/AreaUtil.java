package com.onek.util.area;

import util.MathUtil;

/**
 * 地区工具类。
 */
public class AreaUtil {
    /* 最大层级。省-0；市-1；区-2；县-3；街-4 */
    private static final int MAX_LAYER = 4;
    /* 按层级取余值仓库 */
    private static final long[] MOD_LAYER_STORE = {
            1000L * 1000 * 100 * 100 * 100,
            1000L * 1000 * 100 * 100,
            1000L * 1000 * 100,
            1000L * 1000,
            1000L,
    };

    /**
     * 判定c2是否为c1的所有子。
     * @param c1
     * @param c2
     * @return
     */
    public static boolean isChildren(final long c1, final long c2) {
        if (c1 == c2) {
            return true;
        }

        if (getCodeByLayer(c1, 0) != getCodeByLayer(c2, 0)) {
            return false;
        }

        return getLayer(c1) < getLayer(c2);
    }

    /**
     * 获取对应层级的码
     * @param areac
     * @param layer
     * @return
     */
    public static long getCodeByLayer(final long areac, final int layer) {
        areacCheck(areac);

        if (layer >= MAX_LAYER || layer < -1) {
            return areac;
        }

        return areac - areac % MOD_LAYER_STORE[layer + 1];
    }

    public static long[] getAllAncestorCodes(long areac) {
        areacCheck(areac);

        long[] result = new long[getLayer(areac) + 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = getCodeByLayer(areac, i);
        }

        return result;
    }

    /**
     * 地区码校验。验证不通过将抛出异常
     * @param areac
     */
    public static void areacCheck(long areac) {
    }

    /**
     * 获取地区码级数。
     * @return
     */
    public static int getLayer(long areac) {
        areacCheck(areac);

        int currLayer = MAX_LAYER;

        while (currLayer > 0) {
            long a = MOD_LAYER_STORE[currLayer];
            long r = (areac % a);
            if (r > 0) {
                break;
            }

            currLayer--;
        }

        return currLayer;
    }

    /**
     * 获取地区码父级。
     * @return
     */
    public static long getParent(long areac) {
        areacCheck(areac);

        int layer = getLayer(areac);

        return getCodeByLayer(areac, layer - 1);
    }

    /**
     * 判断是否为最大级。
     * @return
     */
    public static boolean isRoot(long areac) {
        areacCheck(areac);

        return getLayer(areac) == 0;
    }


}
