package com.onek.global.area;

import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;

import java.util.List;

public class AreaStore {
    private static final String AREA_SELECT_BASE = " SELECT areac, arean, cstatus, fee, lcareac ";
    private static final String AREA_WHERE_BASE = " WHERE cstatus&1 = 0 AND areac = ? ";
    private static final String AREA_WHERE_CHILDREN =
            " WHERE cstatus&1 = 0 AND areac REGEXP ? AND areac NOT REGEXP ? ";


    private static final String[] FROMS =
            {
                    " FROM {{?" + DSMConst.TB_AREA_PCA + "}} ",
                    " FROM {{?" + DSMConst.TB_AREA_VILLAGES + "}} ",
                    " FROM {{?" + DSMConst.TB_AREA_STREET + "}} ",
            };

    private static final String PCA =
            AREA_SELECT_BASE
                    + FROMS[0]
                    + AREA_WHERE_BASE;

    private static final String PCA_C =
            AREA_SELECT_BASE
                    + FROMS[0]
                    + AREA_WHERE_CHILDREN;

    private static final String STREET =
            AREA_SELECT_BASE
                    + FROMS[2]
                    + AREA_WHERE_BASE;

    private static final String STREET_C =
            AREA_SELECT_BASE
                    + FROMS[2]
                    + AREA_WHERE_CHILDREN;

    private static final String VILLAGES =
            AREA_SELECT_BASE
                    + FROMS[1]
                    + AREA_WHERE_BASE;

    private static final String VILLAGES_C =
            AREA_SELECT_BASE
                    + FROMS[1]
                    + AREA_WHERE_CHILDREN;

    private static final String[] SQLS =
            { PCA, PCA, PCA, VILLAGES, STREET };

    private static final String[] SQLCS =
            { PCA_C, PCA_C, PCA_C, VILLAGES_C, STREET_C };

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


    public static String[] getCompleteName(long areac) {
        String[] result = new String[AreaUtil.getLayer(areac) + 1];

        AreaEntity area;
        for (int i = 0; i < result.length; i++) {
            area = getAreaByAreac(AreaUtil.getCodeByLayer(areac, i));

            if (area != null) {
                result[i] = area.getArean();
            }
        }

        return result;
    }

    public static AreaEntity[] getChildren(long areac) {
        int childrenLayer = areac == 0 ? 0 : AreaUtil.getLayer(areac) + 1;

        if (childrenLayer >= SQLCS.length) {
            return new AreaEntity[0];
        }

        String[] params = getChildrenRegexp(areac);

        List<Object[]> queryResult =
                BaseDAO.getBaseDAO().queryNative(SQLCS[childrenLayer],
                        params[0], params[1]);

        AreaEntity[] returnResult = new AreaEntity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, returnResult, AreaEntity.class);

        return returnResult;
    }

    /**
     * 通过areac获取area对象
     * @param areac
     * @return
     */
    public static AreaEntity getAreaByAreac(long areac) {
        int layer = AreaUtil.getLayer(areac);

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(SQLS[layer], areac);

        if (queryResult.isEmpty()) {
            return null;
        }

        AreaEntity[] tArray = new AreaEntity[queryResult.size()];
        BaseDAO.getBaseDAO().convToEntity(queryResult, tArray, AreaEntity.class);

        return tArray[0];
    }

    private static String[] getChildrenRegexp(long areac) {
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
