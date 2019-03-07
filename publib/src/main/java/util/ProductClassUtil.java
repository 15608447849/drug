package util;

public class ProductClassUtil {
    private static final int LAYER_BASE = 100;
    private static final int MAX_LAYER = 1;

    /* 按层级取余值仓库 */
    private static final int[] MOD_LAYER_STORE = {
            LAYER_BASE * LAYER_BASE,
            LAYER_BASE,
    };

    /**
     * 获取对应层级的码
     * @param code
     * @param layer
     * @return
     */
    private static Integer getCodeByLayer(final Integer code, final int layer) {
        int codeLayer = getLayer(code);

        if (layer >= codeLayer || layer < -1) {
            return code;
        }

        return code - code % MOD_LAYER_STORE[layer + 1];
    }

    public static int getLayer(Integer code) {
        int currLayer = MAX_LAYER;

        while (currLayer > 0) {
            if ((code % MOD_LAYER_STORE[currLayer]) > 0) {
                break;
            }

            currLayer--;
        }

        return currLayer;
    }

    public static boolean isRoot(Integer code) {
        return getLayer(code) == 0;
    }

    public static Integer getParent(Integer code) {
        int layer = getLayer(code);

        return getCodeByLayer(code, layer - 1);
    }

    public static Integer getAncestorCode(Integer code) {
        return getCodeByLayer(code, 0);
    }

    public static Integer[] getAllAncestorCodes(Integer code) {
        Integer[] result = new Integer[getLayer(code) + 1];

        for (int i = 0; i < result.length; i++) {
            result[i] = getCodeByLayer(code, i);
        }

        return result;
    }

}
