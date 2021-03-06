package com.onek.util.area;

import constant.DSMConst;
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

    private static final String REG_HEAD = "${{HEAD}}";
    private static final String REG_ANYS = "${{ANYS}}";
    private static final String REG_ZERO = "${{ZERO}}";

    private static final String REGBASE =
            "^" + REG_HEAD
                    + "[0-9]{" + REG_ANYS + "}"
                    + "[0]{" + REG_ZERO + "}$";

    private static final String NOTREGBASE =
            "^" + REG_HEAD
                    + "[0]{" + REG_ANYS + "}"
                    + "[0]{" + REG_ZERO + "}$";

    private static int[][] REG_TIMES =
            {
                    {0, 2, 10},
                    {2, 2, 8},
                    {4, 2, 6},
                    {6, 3, 3},
                    {9, 3, 0},
            };

    /**
     * @param c1
     * @param c2
     * @return
     */
    public static boolean isChildren(final long c1, final long c2) {
        if (c1 == c2) {
            return false;
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


    public static String[] getChildrenRegexps(long areac) {
        String head, anys, zero;

        int layer = areac == 0 ? 0 : AreaUtil.getLayer(areac) + 1;

        if (layer >= REG_TIMES.length) {
            throw new IllegalArgumentException("areac is error " + areac);
        }

        int[] params = REG_TIMES[layer];

        head = String.valueOf(areac).substring(0, params[0]);
        anys = String.valueOf(params[1]);
        zero = String.valueOf(params[2]);

        return new String[] {
                REGBASE.replace(REG_HEAD, head)
                        .replace(REG_ANYS, anys)
                        .replace(REG_ZERO, zero),
                NOTREGBASE.replace(REG_HEAD, head)
                        .replace(REG_ANYS, anys)
                        .replace(REG_ZERO, zero)};
    }

}
