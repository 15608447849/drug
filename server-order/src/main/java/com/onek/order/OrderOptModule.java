package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.StoreBasicInfo;
import com.onek.entity.AppriseVO;
import com.onek.entity.AsAppVO;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.sun.org.apache.regexp.internal.RE;
import constant.DSMConst;
import dao.BaseDAO;
import global.GenIdUtil;
import redis.util.RedisUtil;
import util.GsonUtils;
import util.ModelUtil;
import util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单其他操作模块
 * @time 2019/4/20 14:27
 **/
public class OrderOptModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    //商品评价
    private static final String INSERT_APPRISE_SQL = "insert into {{?" + DSMConst.TD_TRAN_APPRAISE + "}} "
            + "(unqid,orderno,level,descmatch,logisticssrv,"
            + "content,createtdate,createtime,cstatus,compid,sku) "
            + " values(?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,0,?,"
            + "?)";

    //售后申请
    private static final String INSERT_ASAPP_SQL = "insert into {{?" + DSMConst.TD_TRAN_APPRAISE + "}} "
            + "(orderno,pdno,asno,compid,astype,gstatus,reason,ckstatus,"
            + "ckdesc,invoice,cstatus,apdata,aptime) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "0,CURRENT_DATE,CURRENT_TIME)";

    //更新订单售后状态
    private static final String UPD_ORDER_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?, "
            + " where cstatus&1=0 and orderno=? and ostatus in(1,2)";


    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus=?";

    //更新售后表售后状态
    private static final String UPD_APPRAISE_SQL = "update {{?" + DSMConst.TD_TRAN_APPRAISE + "}} set ckstatus=? "
            + " where cstatus&1=0 and pdno=? ";

    ;


    /* *
     * @description 评价
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/20 14:48
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result insertApprise(AppContext appContext) {
        Result result = new Result();
        Gson gson = new Gson();
        LocalDateTime localDateTime = LocalDateTime.now();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        JsonArray appriseArr = jsonObject.get("appriseArr").getAsJsonArray();
        for (int i = 0; i < appriseArr.size(); i++) {
            AppriseVO appriseVO = gson.fromJson(appriseArr.get(i).toString(), AppriseVO.class);
            params.add(new Object[]{GenIdUtil.getUnqId(), orderNo, appriseVO.getLevel(), appriseVO.getDescmatch(),
                    appriseVO.getLogisticssrv(), appriseVO.getContent(), compid, appriseVO.getSku()});
        }
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNativeSharding(0, localDateTime.getYear(),
                INSERT_APPRISE_SQL, params, params.size()));
        return b ? result.success("评价成功!") : result.fail("评价失败!");
    }

    /* *
     * @description 查询商品评价
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/20 15:07
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result getGoodsApprise(AppContext appContext) {
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
        String json = appContext.param.json;
        Page page = new Page();
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize = appContext.param.pageNumber;
        PageHolder pageHolder = new PageHolder(page);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long sku = jsonObject.get("sku").getAsLong();
        String selectSQL = "select unqid,orderno,level,descmatch,logisticssrv,"
                + "content,createtdate,createtime,cstatus,compid from {{?"
                + DSMConst.TD_TRAN_APPRAISE + "}} where cstatus&1=0 and sku=" + sku;
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, localDateTime.getYear(),
                pageHolder, page, selectSQL);
        AppriseVO[] appriseVOS = new AppriseVO[queryResult.size()];
        baseDao.convToEntity(queryResult, appriseVOS, AppriseVO.class);
        for (AppriseVO appriseVO : appriseVOS) {
            String compStr = RedisUtil.getStringProvide().get(appriseVO.getCompid() + "");
            System.out.println("compStr-->> " + compStr);
            //storeName
            StoreBasicInfo storeBasicInfo = GsonUtils.jsonToJavaBean(compStr, StoreBasicInfo.class);
            if (storeBasicInfo != null) {
                appriseVO.setCompName(storeBasicInfo.storeName);//暂无接口。。。。
            }
        }
        return result.setQuery(appriseVOS, pageHolder);
    }


    /* *
     * @description 售后申请
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/23 14:29
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result afterSaleApp(AppContext appContext) {
        int res = 0;
        boolean b = false;
        Result result = new Result();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        AsAppVO asAppVO = GsonUtils.jsonToJavaBean(json, AsAppVO.class);
        if (asAppVO != null) {
            String orderNo = asAppVO.getOrderno();
            int year = Integer.parseInt("20" + orderNo.substring(0, 2));
            int compid = asAppVO.getCompid();
            int asType = asAppVO.getAstype();//astype 售后类型(0 换货 1退款退货 2 仅退款 3 开发票)
            long pdno = asAppVO.getPdno();//商品SKU
            if (StringUtils.isEmpty(orderNo) || compid <= 0) {
                return result.fail("申请参数有误");
            }
            if (asType == 0 || asType == 1 || asType == 2) {
                //更新订单售后状态
                sqlList.add(UPD_ORDER_SQL);
//                params.add(new Object[]{-1, orderNo});
                getUpdGoodsSql(pdno, sqlList, params, orderNo, compid);
                String[] sqlNative = new String[sqlList.size()];
                sqlNative = sqlList.toArray(sqlNative);
                b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid, year, sqlNative, params));
            }
            if (b) {
                //向售后表插入申请数据
                res = baseDao.updateNative(INSERT_ASAPP_SQL, asAppVO.getOrderno(), asAppVO.getPdno(), asAppVO.getAsno(),
                        asAppVO.getCompid(), asAppVO.getAstype(), asAppVO.getGstatus(), asAppVO.getReason(),
                        asAppVO.getCkstatus(), asAppVO.getCkdesc(), asAppVO.getInvoice());
            }
        }

        return res > 0 ? result.success("申请成功") : result.fail("申请失败");
    }

    /* *
     * @description 更新订单下商品售后状态
     * @params [asType, sqlList, params]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/23 14:54
     * @version 1.1.1
     **/
    private void getUpdGoodsSql(long sku, List<String> sqlList, List<Object[]> params, String orderNo, int compid) {
        if (sku == 0) {
            TranOrderGoods[] tranOrderGoods = TranOrderOptModule.getGoodsArr(orderNo, compid);
            for (TranOrderGoods tog :tranOrderGoods) {
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1,tog.getPdno(),0});
            }
        } else {
            sqlList.add(UPD_ORDER_GOODS_SQL);
            params.add(new Object[]{1,sku,0});
        }
    }

    /* *
     * @description 取消售后
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/23 15:41
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result cancelAfterSale(AppContext appContext) {
        Result result = new Result();
//        List<String> sqlList = new ArrayList<>();
//        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        int compId = jsonObject.get("compid").getAsInt();
        long sku = jsonObject.get("sku").getAsLong();
//        String updOrderSql = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set "
//                + "asstatus=? where cstatus&1=0 and orderno=? and asstatus in(1,2,3)";
        //更新订单商品售后状态
        String updOrderGoodsSql = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
                + " where cstatus&1=0 and pdno=? and asstatus in(1,2,3)";
        int resultCode = baseDao.updateNativeSharding(compId, year, updOrderGoodsSql, -1, sku);

        if (resultCode > 0) {
            //更新售后表状态
            resultCode = baseDao.updateNative(UPD_APPRAISE_SQL, -2, sku);
        }
        return resultCode > 0 ? result.success("取消成功") : result.fail("取消失败");
//        sqlList.add(updOrderSql);
//        params.add(new Object[]{-1, orderNo});
//        TranOrderGoods[] tranOrderGoods = TranOrderOptModule.getGoodsArr(orderNo, compId);
//        for (TranOrderGoods tog :tranOrderGoods) {
//            sqlList.add(updOrderGoodsSql);
//            params.add(new Object[]{-1, tog.getPdno(), 0});
//        }
//        String[] sqlNative = new String[sqlList.size()];
//        sqlNative = sqlList.toArray(sqlNative);
//        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compId, year, sqlNative, params));
//        return b ? result.success("取消成功") : result.fail("取消失败");
    }
}
