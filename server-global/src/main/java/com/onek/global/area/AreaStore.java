package com.onek.global.area;

import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;
import util.ArrayUtil;

import java.util.Arrays;
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

        String[] params = AreaUtil.getChildrenRegexps(areac);

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

      /**
           *
           * 功能: 获取所有城市(市级别) (特殊地：四大直辖和自治区直辖县级行政区划为特殊值 注意区分)
           * 参数类型:
           * 参数集:
           * 返回值:
           * 详情说明:
           * 日期: 2019/6/6 14:47
           * 作者: Helena Rubinstein
           */
      
    public static AreaEntity[] getAllCities() {
        String sql = AREA_SELECT_BASE + FROMS[0]
                + " WHERE cstatus&1 = 0 AND areac REGEXP ? AND areac NOT REGEXP ? "
                + " AND areac NOT REGEXP ? AND areac NOT IN (110100000000, 120100000000, 310100000000, 500100000000) ";
        
        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql,
                "[1-9]{1}[0-9]{3}[0]{8}", "[0-9]{2}[0]{10}", "[0-9]{2}90[0]{8}");

        AreaEntity[] returnResult = new AreaEntity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, returnResult, AreaEntity.class);

        // 加入特殊值 (四大直辖和自治区直辖县级行政区划)
        String findSP = AREA_SELECT_BASE + FROMS[0]
                + " WHERE cstatus&1 = 0 "
                + " AND ((areac NOT REGEXP ? AND areac LIKE ?) "
                + " OR areac IN (110000000000, 120000000000, 310000000000, 500000000000)) ";

        queryResult = BaseDAO.getBaseDAO().queryNative(findSP, "[0-9]{2}90[0]{8}", "__90%");

        AreaEntity[] spResult = new AreaEntity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, spResult, AreaEntity.class);

        return ArrayUtil.concat(returnResult, spResult);
    }

    /**
     *
     * 功能: 获取所有城市(市级别) (特殊地：四大直辖和自治区直辖县级行政区划为特殊值 注意区分)
     * 参数类型:
     * 参数集:
     * 返回值:
     * 详情说明:
     * 日期: 2019/6/6 14:47
     * 作者: Helena Rubinstein
     */

    public static AreaEntity[] getAllArea() {

        String sqls = AREA_SELECT_BASE + FROMS[0]
                + " WHERE cstatus&1 = 0 "
                + " AND areac NOT IN (110100000000, 120100000000, 310100000000, 500100000000) ";

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sqls);

        AreaEntity[] returnResult = new AreaEntity[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, returnResult, AreaEntity.class);

        return returnResult;
    }

}
