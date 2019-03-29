package com.onek.util.produce;

import util.StringUtils;

public class ProduceClassUtil {
    private static final String ZERO_STR = "0";
    private static final int MAX_LAYER = 3;
    private static final int LAYER_NUM = 2;
    /**
     * 获取对应层级的码
     * @param pclass
     * @param layer
     * @return
     */
    public static String getCodeByLayer(final String pclass, final int layer) {
        int currLayer = getLayer(pclass);
        int gapLayer = currLayer - layer;

        if (gapLayer == 0) {
            return pclass;
        } else if (gapLayer < 0) {
            return fillZero(pclass, -gapLayer * LAYER_NUM);
        } else {
            return cutStr(pclass, gapLayer * LAYER_NUM);
        }

    }

    public static String[] getAllAncestorCodes(String pclass) {
        String[] result = new String[getLayer(pclass) + 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = getCodeByLayer(pclass, i);
        }

        return result;
    }

    /**
     * 获取祖先码。
     * @param pclass
     * @return
     */
    public static String getAncestorCode(String pclass) {
        return getCodeByLayer(pclass, 0);
    }


    /**
     * 获取地区码级数。
     * @return
     */
    public static int getLayer(String pclass) {
        int currLayer = 0;

        while (currLayer < MAX_LAYER) {
            if (StringUtils.isEmpty(pclass = cutStr(pclass, LAYER_NUM))) {
                break;
            }

            currLayer++;
        }

        return currLayer;
    }

    /**
     * 获取地区码父级。
     * @return
     */
    public static String getParent(String pclass) {
        int layer = getLayer(pclass);

        return getCodeByLayer(pclass, layer - 1);
    }

    /**
     * 判断是否为最大级。
     * @return
     */
    public static boolean isRoot(String pclass) {
        return getLayer(pclass) == 0;
    }

    private static String fillZero(String str, int times) {
        StringBuilder sb = new StringBuilder(str);

        for (int i = 0; i < times; i++) {
            sb.append(ZERO_STR);
        }

        return sb.toString();
    }

    private static String cutStr(String str, int times) {
        int cut = str.length() - times;

        return cut <= 0 ? "" : str.substring(0, cut);
    }

}
