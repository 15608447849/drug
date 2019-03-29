package com.onek.util.area;

import util.MathUtil;

/**
 * 地区工具类。
 */
public class AreaUtil {
    /* 省市区级最大位数基数 */
    private static final int LAYER_BASE = 100;
    /* 最小地区码 */
    private static final int MIN_AREAC = 110000;
    /* 最大地区码 */
    private static final int MAX_AREAC = 820104;
    /* 最大层级。省-0；市-1；区-2 */
    private static final int MAX_LAYER = 2;
    /* 按层级取余值仓库 */
    private static final int[] MOD_LAYER_STORE = {
            LAYER_BASE * LAYER_BASE * LAYER_BASE,
            LAYER_BASE * LAYER_BASE,
            LAYER_BASE,
    };

    /**
     * 是否存在血缘关系
     * @param areac1
     * @param areac2
     * @return
     */
    public static boolean isRelationship(int areac1, int areac2) {
        areacCheck(areac1);
        areacCheck(areac2);

        return getAncestorCode(areac1) == getAncestorCode(areac2);
    }

    /**
     * 获取对应层级的码
     * @param areac
     * @param layer
     * @return
     */
    public static int getCodeByLayer(final int areac, final int layer) {
        areacCheck(areac);

        if (layer >= MAX_LAYER || layer < -1) {
            return areac;
        }

        return areac - areac % MOD_LAYER_STORE[layer + 1];
    }

    public static int[] getAllAncestorCodes(int areac) {
        areacCheck(areac);

        int[] result = new int[getLayer(areac) + 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = getCodeByLayer(areac, i);
        }

        return result;
    }

    /**
     * 获取祖先码。
     * @param areac
     * @return
     */
    public static int getAncestorCode(int areac) {
        areacCheck(areac);

        return getCodeByLayer(areac, 0);
    }

    /**
     * 地区码校验。验证不通过将抛出异常
     * @param areac
     */
    public static void areacCheck(int areac) {
        if (!MathUtil.isBetween(MIN_AREAC, areac, MAX_AREAC)) {
            throw new IllegalArgumentException("The areac is Error, which is " + areac);
        }
    }

    /**
     * 获取地区码级数。
     * @return
     */
    public static int getLayer(int areac) {
        areacCheck(areac);

        int currLayer = MAX_LAYER;

        while (currLayer > 0) {
            if ((areac % MOD_LAYER_STORE[currLayer]) > 0) {
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
    public static int getParent(int areac) {
        areacCheck(areac);

        int layer = getLayer(areac);

        return getCodeByLayer(areac, layer - 1);
    }

    /**
     * 判断是否为最大级。
     * @return
     */
    public static boolean isRoot(int areac) {
        areacCheck(areac);

        return getLayer(areac) == 0;
    }

}
