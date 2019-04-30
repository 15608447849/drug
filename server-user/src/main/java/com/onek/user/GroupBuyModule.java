package com.onek.user;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;
import util.StringUtils;

import java.util.List;

public class GroupBuyModule {
    private final static String COUNT_GROUP_NUM =
            " SELECT COUNT(DISTINCT compid) "
            + " FROM {{?" + DSMConst.TD_PROM_GROUP + "}} g, "
            + " {{?" + DSMConst.TD_PROM_ACT + "}} a, "
            + " {{?" + DSMConst.TD_PROM_TIME + "}} t "
            + " WHERE g.cstatus&1 = 0 AND a.cstatus&1 = 0 AND t.cstatus&1 = 0 "
            + " AND g.actcode = a.unqid AND a.unqid = t.actcode "
            + " AND g.joindate BETWEEN a.sdate AND a.edate "
            + " AND g.jointime BETWEEN t.sdate AND t.edate "
            + " AND g.actcode = ?";

    @UserPermission(ignore = true)
    public Result getGroupCount(AppContext appContext) {
        String[] arrays = appContext.param.arrays;

        if (ArrayUtil.isEmpty(arrays)) {
            return new Result().fail("参数为空");
        }

        String actCode = arrays[0];

        if (!StringUtils.isBiggerZero(actCode)) {
            return new Result().fail("非法参数");
        }

        List<Object[]> queryResult =
                BaseDAO.getBaseDAO().queryNative(COUNT_GROUP_NUM, actCode);

        return new Result().success(Integer.parseInt(queryResult.get(0)[0].toString()));
    }
}
