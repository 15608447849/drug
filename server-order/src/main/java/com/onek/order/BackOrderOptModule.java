package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.context.AppContext;
import com.onek.entity.AsAppVO;
import com.onek.entitys.Result;
import com.onek.util.dict.DictStore;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import util.MathUtil;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BackOrderOptModule {
    private static final String QUERY_ASAPP_BASE =
            " SELECT orderno, pdno, asno, compid, astype, "
            + " gstatus, reason, ckstatus, ckdate, cktime, "
            + " ckdesc, invoice, cstatus, apdata, aptime, "
            + " apdesc, checker, refamt, asnum, checkern, compn, invoicetype, pkgno "
            + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
            + " WHERE cstatus&1 = 0 ";
    /**
     * @接口摘要 查询售后列表
     * @业务场景 查询售后列表
     * @传参类型 arrays
     * @传参列表 [订单号, sku, 售后单号, 企业码, 售后类型, 货物状态, 原因, 审核状态, 审核时间起, 审核时间止, 申请时间起, 申请时间止]
     * @返回列表 code=200 data=结果信息
     */
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

        ProdEntity prod;
        for (AsAppVO asAppVO : result) {
            asAppVO.setRefamt(MathUtil.exactDiv(asAppVO.getRefamt(), 100.0).doubleValue());

            try {
                asAppVO.setReasonName(DictStore.getDictById(asAppVO.getReason()).getText());
            } catch (Exception e) {}

            try {
                prod = ProdInfoStore.getProdBySku(asAppVO.getPdno());
            } catch (Exception e) { prod=null; }

            if (prod != null) {
                asAppVO.setProdname(prod.getProdname());
                asAppVO.setSpec(prod.getSpec());
                asAppVO.setBrandname(prod.getBrandName());
                asAppVO.setManuname(prod.getManuName());
                asAppVO.setSpu(String.valueOf(asAppVO.getPdno())
                        .substring(0, 12));
            }
        }

        return new Result().setQuery(result, pageHolder);
    }

    public Result getAppDetail(AppContext appContext) {
        return queryAsapp(appContext);
    }

}
