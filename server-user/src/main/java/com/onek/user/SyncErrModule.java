package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.SyncErrVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncErrModule {
    private static final String QUERY_BASE =
            " SELECT unqid, synctype, syncid, syncmsg, cstatus, "
                + " syncdate, synctime, syncfrom, syncreason, synctimes, "
                + " syncway "
            + " FROM {{?" + DSMConst.TD_SYNC_ERROR + "}} "
            + " WHERE cstatus&1 = 0 ";

    @UserPermission(ignore = true)
    public Result querySyncErr(AppContext appContext) {
        String[] params = appContext.param.arrays;
        StringBuilder sql =  new StringBuilder(QUERY_BASE);

        List<Object> paramList = new ArrayList<>();

        String param;
        for (int i = 0; i < params.length; i++) {
            param = params[i];

            if (StringUtils.isEmpty(param)) {
                continue;
            }

            try {
                switch (i) {
                    case 0:
                        sql.append(" AND synctype = ? ");
                        break;
                    case 1:
                        sql.append(" AND syncreason = ? ");
                        break;
                    case 2:
                        sql.append(" AND syncdate >= ? ");
                        break;
                    case 3:
                        sql.append(" AND syncdate <= ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(pageHolder, page,
                " syncdate DESC, synctime DESC ", sql.toString(), paramList.toArray());

        SyncErrVO[] results = new SyncErrVO[queryResult.size()];
        
        BaseDAO.getBaseDAO().convToEntity(queryResult, results, SyncErrVO.class);

        return new Result().setQuery(JSON.toJSON(results), pageHolder);
    }

}
