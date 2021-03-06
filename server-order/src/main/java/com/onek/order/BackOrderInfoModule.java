package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.BDOrderAchieveemntVO;
import com.onek.entity.TranOrder;
import com.onek.entitys.Result;
import com.onek.op.BDOrderAchieveementOP;
import com.onek.op.ExcelOrdersInfoOP;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.MathUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BackOrderInfoModule {
    private static final String QUERY_TRAN_ORDER_PARAMS =
            " ord.orderno, ord.tradeno, ord.cusno, ord.busno, ord.ostatus, "
                    + " ord.asstatus, ord.pdnum, ord.pdamt, ord.freight, ord.payamt, "
                    + " ord.coupamt, ord.distamt, ord.rvaddno, ord.shipdate, ord.shiptime, "
                    + " ord.settstatus, ord.settdate, ord.setttime, ord.otype, ord.odate, "
                    + " ord.otime, ord.cstatus, ord.consignee, ord.contact, ord.address, ord.balamt, ord.payway, ord.remarks, ord.invoicetype ";

    private static final String FROM_BK_ORDER = " {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} ord ";

    private static final String QUERY_ORDER_BASE =
            " SELECT " + QUERY_TRAN_ORDER_PARAMS
                    + " FROM " + FROM_BK_ORDER
                    + " WHERE 1=1 ";

    /**
     * @接口摘要 查询后台订单列表
     * @业务场景 查询后台订单列表
     * @传参类型 arrays
     * @传参列表 [卖家码，买家码，订单状态，订单号，订单时间起，订单时间止]
     * @返回列表 code=200 data=结果信息
     */
    public Result queryOrders(AppContext appContext) {
        String[] params = appContext.param.arrays;

        if (params == null || params.length == 0) {
            return new Result().fail("参数为空");
        }

        int compid = Integer.parseInt(params[0]);

        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;

        PageHolder pageHolder = new PageHolder(page);

        TranOrder[] result = queryOrders(params, compid, pageHolder, page);

        return new Result().setQuery(result, pageHolder);
    }

    private TranOrder[] queryOrders(String[] params, int compid,
                                    PageHolder pageHolder, Page page) {
        StringBuilder sql = new StringBuilder(QUERY_ORDER_BASE);

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
                        sql.append(" AND ord.busno = ? ");
                        break;
                    case 1:
                        sql.append(" AND ord.cusno = ? ");
                        break;
                    case 2:
                        sql.append(" AND ord.ostatus = ? ");
                        break;
                    case 3:
                        sql.append(" AND ord.orderno = ? ");
                        break;
                    case 4:
                        sql.append(" AND ord.odate >= ? ");
                        break;
                    case 5:
                        sql.append(" AND ord.odate <= ? ");
                        break;
                }
            } catch (Exception e) {
                continue;
            }

            paramList.add(param);
        }

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNativeSharding(
                compid, TimeUtils.getCurrentYear(), pageHolder, page,
                " ord.oid DESC ", sql.toString(), paramList.toArray());

        TranOrder[] result = new TranOrder[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, result, TranOrder.class);

        Map<String, String> compMap;
        for (TranOrder tranOrder : result) {
            String compStr = IceRemoteUtil.getCompInfoByCacheOrSql(tranOrder.getCusno());

            compMap = GsonUtils.string2Map(compStr);

            if (compMap != null) {
                tranOrder.setCusname(compMap.get("storeName"));
            }

//            tranOrder.setGoods(getOrderGoods(tranOrder.getOrderno(), compid));
            tranOrder.setPayamt(MathUtil.exactDiv(tranOrder.getPayamt(), 100).doubleValue());
            tranOrder.setFreight(MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue());
            tranOrder.setPdamt(MathUtil.exactDiv(tranOrder.getPdamt(), 100).doubleValue());
            tranOrder.setDistamt(MathUtil.exactDiv(tranOrder.getDistamt(), 100).doubleValue());
            tranOrder.setCoupamt(MathUtil.exactDiv(tranOrder.getCoupamt(), 100).doubleValue());
            tranOrder.setBalamt(MathUtil.exactDiv(tranOrder.getBalamt(), 100).doubleValue());
        }

        return result;
    }

    /**
     * 导出订单信息
     * @param appContext
     * @return
     */
    public Result excelOrdersInfo(AppContext appContext){
        TranOrder[] tranOrders = (TranOrder[]) queryOrders(appContext).data;
        return new ExcelOrdersInfoOP(tranOrders).excelOrderInfo();
    }


    /**
     * 远程获取所有用户信息
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result getBDOrderAchieveementInfo(AppContext appContext){
        List<BDOrderAchieveemntVO> list = BDOrderAchieveementOP.executeOrderInfos(appContext);
        return new Result().success(list);
    }

    @UserPermission(ignore =  true)
    public Result getCumultive(AppContext appContext){
        String str[] = appContext.param.arrays;
        Map map = BDOrderAchieveementOP.getCumulative(str[0]);
        return new Result().success(map);
    }


    @UserPermission(ignore =  true)
    public Result getNewAddCumulative(AppContext appContext){
        String str[] = appContext.param.arrays;
        appContext.logger.print("=====start==="+str[0]+"=========end======"+str[1]);
        Map map = BDOrderAchieveementOP.getNewAddCumulative(str);
        return new Result().success(map);
    }

    /**
     * 查询报表明细
     * @param appContext
     * @return
     */
    public Result getBDUserOrderInfo(AppContext appContext){
        return BDOrderAchieveementOP.getBDUserOrderInfo(appContext);
    }

    /**
     * 查询首购累计以及新增首购
     * @return
     */
    public Result getUserSGAndXZSG(AppContext appContext){
        return BDOrderAchieveementOP.getBDUserCumultive(appContext);
    }
}
