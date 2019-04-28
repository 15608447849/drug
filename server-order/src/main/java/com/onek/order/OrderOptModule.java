package com.onek.order;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.AppriseVO;
import com.onek.entity.AsAppVO;
import com.onek.entity.TranOrderGoods;
import com.onek.entitys.Result;
import com.onek.util.LccOrderUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GenIdUtil;
import util.*;

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
    private static final String INSERT_ASAPP_SQL = "insert into {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
            + "(orderno,pdno,asno,compid,astype,gstatus,reason,ckstatus,"
            + "ckdesc,invoice,cstatus,apdata,aptime,apdesc,refamt,asnum) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "0,CURRENT_DATE,CURRENT_TIME,?,?,?)";

    //更新订单售后状态
    private static final String UPD_ORDER_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus in(1,2,3)";


    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus=? and orderno=?";

    //更新售后表售后状态
    private static final String UPD_ASAPP_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=? "
            + " where cstatus&1=0 and pdno=? ";


    //更新售后表售后审核状态(-1－拒绝； 0－未审核 ；1－审核通过)
    private static final String UPD_ASAPP_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=?, "
            + "ckdate = CURRENT_DATE,cktime = CURRENT_TIME,checker = ?,reason = ? where cstatus&1=0 and asno=? ";





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
        boolean b = false;
        Result result = new Result();
        LocalDateTime localDateTime = LocalDateTime.now();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        List<Object[]> paramsInsert = new ArrayList<>();
        List<AsAppVO> asAppVOS = new ArrayList<>();//sku集合
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        JsonArray asAppArr = jsonObject.get("asAppArr").getAsJsonArray();
        String orderNo = jsonObject.get("orderno").getAsString();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        int compid = appContext.getUserSession().compId;
        int asType = jsonObject.get("asType").getAsInt();//astype 售后类型(0 换货 1退款退货 2 仅退款 3 开发票)
        if (StringUtils.isEmpty(orderNo) || compid <= 0) {
            return result.fail("申请参数有误");
        }
        for (int i = 0; i < asAppArr.size(); i++) {
            AsAppVO asAppVO = GsonUtils.jsonToJavaBean(asAppArr.get(i).toString(), AsAppVO.class);
            if (asAppVO != null) {
                asAppVO.setAstype(asType);
                asAppVOS.add(asAppVO);
            }
        }
        if (doApply(asAppVOS, orderNo)) {
            return result.fail("该订单售后申请正在处理中！");
        }

        if (asType == 0 || asType == 1 || asType == 2) {
            //更新订单售后状态
            sqlList.add(UPD_ORDER_SQL);
            params.add(new Object[]{-1, orderNo});
            getUpdGoodsSql(asAppVOS, sqlList, params, orderNo, compid);
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid, year, sqlNative, params));
        }
        if (b) {
            //向售后表插入申请数据
            getInsertSql(asAppVOS, paramsInsert);
            b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNativeSharding(0,localDateTime.getYear(),
                    INSERT_ASAPP_SQL, paramsInsert, asAppVOS.size()));
        }

        return b ? result.success("申请成功") : result.fail("申请失败");
    }

    private boolean doApply(List<AsAppVO> asAppVOS, String orderNo) {
        LocalDateTime localDateTime = LocalDateTime.now();
        StringBuilder skuBuilder = new StringBuilder();
        for (AsAppVO asAppVO : asAppVOS) {
            skuBuilder.append(asAppVO.getPdno()).append(",");
        }
        String pdnoStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
        String sql = "select count(*) from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where cstatus&1=0 "
                + " and orderno=" + orderNo + " and ckstatus=0 and astype<>3 and pdno in(" + pdnoStr + ")";
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, localDateTime.getYear(), sql);
        return (long)queryResult.get(0)[0] > 0;

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
    private void getUpdGoodsSql(List<AsAppVO> asAppVOS, List<String> sqlList, List<Object[]> params,
                                String orderNo, int compid) {
        List<Long> skuList = new ArrayList<>();
        TranOrderGoods[] tranOrderGoods = TranOrderOptModule.getGoodsArr(orderNo, compid);
        for (AsAppVO asAppVO : asAppVOS) {
            for (TranOrderGoods tranOrderGoods1:tranOrderGoods) {
                if (asAppVO.getPdno() == tranOrderGoods1.getPdno()){
                    double payamt = tranOrderGoods1.getPayamt()/100;
                    double unitP = MathUtil.exactDiv(payamt, tranOrderGoods1.getPnum()).doubleValue();
                    asAppVO.setRefamt(MathUtil.exactMul(unitP, asAppVO.getAsnum()).doubleValue());
                }
                if (asAppVO.getPdno() == tranOrderGoods1.getPdno() && asAppVO.getAsnum() == tranOrderGoods1.getPnum()){
                    skuList.add(asAppVO.getPdno());
                }
            }
        }
        if (skuList.size() == tranOrderGoods.length) {
            for (TranOrderGoods tog : tranOrderGoods) {
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1,tog.getPdno(),0, orderNo});
            }
        } else {
            for (AsAppVO asAppVO : asAppVOS) {
                long sku = asAppVO.getPdno();
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1, sku, 0, orderNo});
            }
        }
    }

    /* *
     * @description 获取新增sql
     * @params [asAppVOS, paramsInsert]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/4/26 11:10
     * @version 1.1.1
     **/
    private void getInsertSql(List<AsAppVO> asAppVOS, List<Object[]> paramsInsert) {
        for (AsAppVO asAppVO : asAppVOS) {
            paramsInsert.add(new Object[]{asAppVO.getOrderno(), asAppVO.getPdno(), asAppVO.getAsno(),
                    asAppVO.getCompid(), asAppVO.getAstype(), asAppVO.getGstatus(), asAppVO.getReason(),
                    asAppVO.getCkstatus(), asAppVO.getCkdesc(), asAppVO.getInvoice(),asAppVO.getApdesc(),
                    asAppVO.getRefamt() * 100, asAppVO.getAsnum()});
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
        LocalDateTime localDateTime = LocalDateTime.now();
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
            resultCode = baseDao.updateNativeSharding(0,localDateTime.getYear(),
                    UPD_ASAPP_SQL, -2, sku);
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

    @UserPermission(ignore = true)
    public Result getLogisticsInfo(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.has("orderno") ? jsonObject.get("orderno").getAsString() : "";
//        int compid = jsonObject.has("compid") ?  jsonObject.get("compid").getAsInt() : 0;

        JSONObject result = LccOrderUtil.queryTraceByOrderno(orderNo);
        return new Result().success(result);
    }


    /* *
     * @description 售后申请审核
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result afterSaleReview(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        if(userSession == null || (userSession.roleCode & 128 )== 0){
            return result.fail("当前用户没有该权限");
        }
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int asno = jsonObject.get("asno").getAsInt();
        String reason = jsonObject.get("reason").getAsString();
        int type =  jsonObject.get("type").getAsInt();
        int astype =  jsonObject.get("astype").getAsInt();
        int ret = baseDao.updateNativeSharding(0,
                TimeUtils.getCurrentYear(),UPD_ASAPP_CK_SQL, type, userSession.userId, reason, asno);
        return ret > 0 ? result.success("操作成功") : result.fail("操作失败");
    }

    /* *
     * @description 删除订单
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/24 11:04
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result deleteOrder(AppContext appContext) {
        Result result = new Result();
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compId = userSession.compId;
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String updateOrderSql = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and ostatus in(-4,3) and orderno=" + orderNo;
        int re = baseDao.updateNativeSharding(compId, year, updateOrderSql);
//        sqlList.add(updateOrderSql);
//        params.add(new Object[]{});
//        String updateGoodsSql = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set cstatus=cstatus|1 where "
//                + " cstatus&1=0 and orderno=" + orderNo;
//        sqlList.add(updateGoodsSql);
//        params.add(new Object[]{});
//        String[] sqlNative = new String[sqlList.size()];
//        sqlNative = sqlList.toArray(sqlNative);
//        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compId, year, sqlNative, params));
        return re > 0 ? result.success("删除成功") : result.fail("删除失败");
    }

}
