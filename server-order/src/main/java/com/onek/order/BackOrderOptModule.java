package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entity.AsAppVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackOrderOptModule {
    private static final String QUERY_ASAPP_BASE =
            " SELECT orderno, pdno, asno, compid, astype, "
            + " gstatus, reason, ckstatus, ckdate, cktime, "
            + " ckdesc, invoice, cstatus, apdata, aptime "
            + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
            + " WHERE cstatus&1 = 0 ";

    public Result queryAsapp(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null) {
            return new Result().fail("参数为空");
        }

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        StringBuilder sql = new StringBuilder(QUERY_ASAPP_BASE);

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
                        sql.append(" AND orderno = ? ");
                        break;
                    case 1:
                        sql.append(" AND pdno = ? ");
                        break;
                    case 2:
                        sql.append(" AND asno = ? ");
                        break;
                    case 3:
                        sql.append(" AND compid = ? ");
                        break;
                    case 4:
                        sql.append(" AND astype = ? ");
                        break;
                    case 5:
                        sql.append(" AND gstatus = ? ");
                        break;
                    case 6:
                        sql.append(" AND reason = ? ");
                        break;
                    case 7:
                        sql.append(" AND ckstatus = ? ");
                        break;
                    case 8:
                        sql.append(" AND ckdate >= ? ");
                        break;
                    case 9:
                        sql.append(" AND ckdate <= ? ");
                        break;
                    case 10:
                        sql.append(" AND apdata >= ? ");
                        break;
                    case 11:
                        sql.append(" AND apdata <= ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(
                pageHolder, page,
                " apdata DESC, aptime DESC ",
                sql.toString(), paramList.toArray());

        AsAppVO[] result = new AsAppVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, AsAppVO.class);

        return new Result().setQuery(result, pageHolder);
    }

}
