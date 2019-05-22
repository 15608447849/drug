package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.context.UserSession;
import com.onek.entity.*;
import com.onek.entitys.Result;
import com.onek.util.*;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdInfoStore;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.onek.order.TranOrderOptModule.CONFIRM_RECEIPT;
import static constant.DSMConst.TD_BK_TRAN_GOODS;

/**
 * @author 11842
 * @version 1.1.1
 * @description 订单其他操作模块
 * @time 2019/4/20 14:27
 **/
public class OrderOptModule {
    private static BaseDAO baseDao = BaseDAO.getBaseDAO();


    //售后申请
    private static final String INSERT_ASAPP_SQL = "insert into {{?" + DSMConst.TD_TRAN_ASAPP + "}} "
            + "(orderno,pdno,asno,compid,astype,gstatus,reason,ckstatus,"
            + "ckdesc,invoice,cstatus,apdata,aptime,apdesc,refamt,asnum) "
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,CURRENT_DATE,CURRENT_TIME,?,?,?)";

    //更新订单售后状态
    private static final String UPD_ORDER_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus in(3,4)";


    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus=? and orderno=? and compid = ? ";

    //更新售后表售后状态
    private static final String UPD_ASAPP_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=?,"
            + " where cstatus&1=0 and pdno=? and orderno = ? and compid = ?";



    //更新售后表售后审核状态(-1－拒绝； 0－未审核 ；1－审核通过)
    private static final String UPD_ASAPP_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus=?, "
            + "ckdate = CURRENT_DATE,cktime = CURRENT_TIME,checker = ?,checkern = ?,astype = ?,ckdesc = ?,refamt = ?" +
            " where cstatus&1=0 and  ckstatus = 0 and asno=?  ";


    //查询售后详情
    private static final String QUERY_ASAPP_INFO_SQL = " select asapp.orderno,asapp.compid,asapp.asno,asapp.pdno,"+
            "asapp.asnum,goods.pdprice/100 spdprice,"+
            "goods.payamt/100 spayamt,distprice/100 sdistprice,goods.pnum,astype,reason,apdesc,refamt/100 refamt,"+
            "ckstatus,ckdesc,gstatus,ckdate,cktime,apdata,aptime,asapp.cstatus,goods.balamt/100 balamt "+
            " from {{?"+ DSMConst.TD_TRAN_ASAPP+"}} asapp inner join {{?"+
            TD_BK_TRAN_GOODS+"}} goods on asapp.orderno = goods.orderno and "+
            " asapp.pdno = goods.pdno where asapp.asno = ? and asno != 0 ";


    //查询售后列表
    private static final String QUERY_ASAPP_LIST_SQL = "  select distinct asapp.orderno,asapp.compid,asapp.asno,astype,"+
            "ckstatus,gstatus,apdata,aptime,checkern,contact,address,refamt/100 refamt,compn from "+
            " {{?"+ DSMConst.TD_TRAN_ASAPP+"}} asapp inner join {{?"+
            TD_BK_TRAN_GOODS+"}} goods on asapp.orderno = goods.orderno and "+
            " asapp.pdno = goods.pdno and asapp.compid = goods.compid " +
            " inner join {{?" +DSMConst.TD_BK_TRAN_ORDER+"}} orders" +
            " on orders.orderno = goods.orderno where asapp.cstatus & 1 = 0 and asno != 0 ";

    //查询发票售后详情
    private static final String QUERY_ASAPP_INVOICE_INFO_SQL = "  select asapp.orderno,asapp.compid,asapp.asno,asapp.pdno,asapp.asnum," +
            "astype,reason,apdesc,refamt,ckstatus,ckdesc,gstatus,ckdate,cktime,apdata,aptime,asapp.cstatus,invoice " +
            " from {{?"+ DSMConst.TD_TRAN_ASAPP+"}} asapp  inner join {{?" +DSMConst.TD_BK_TRAN_ORDER+"}} orders " +
            " on asapp.orderno = orders.orderno where asapp.cstatus & 1 = 0 and asapp.asno = ? and asno != 0";

    //查询发票售后详情
    private static final String QUERY_GOODS_SQL = " select goods.pdno,goods.pnum,goods.pdprice,goods.distprice,goods.payamt from {{?"+
            TD_BK_TRAN_GOODS+"}} goods where goods.orderno = ? and goods.compid = ?";

    //查询售后发票列表
    private static final String QUERY_ASAPP_BILL_LIST_SQL = "select distinct asapp.orderno,asapp.compid,asapp.asno,astype,"+
            "ckstatus,gstatus,apdata,aptime,checkern,contact,address,compn from "+
            " {{?"+ DSMConst.TD_TRAN_ASAPP+"}} asapp " +
            " inner join {{?" +DSMConst.TD_BK_TRAN_ORDER+"}} orders" +
            " on asapp.orderno = orders.orderno where asno != 0 and asapp.cstatus & 1 = 0 and asapp.astype in (3,4) ";


    //查询团购订单记录
    private static final String QUERY_TEAM_BUY_ORDER_SQL = "select orderno,payamt,compid,pnum from "+
            " {{?"+ TD_BK_TRAN_GOODS+"}} g " +
            " where g.createdate >= ? and g.createdate <= ? and g.promtype& 4096 > 0 and actcode like concat('%',?,'%')";

    //更新订单售后状态
    private static final String UPD_ORDER_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus = ? ";

    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_CK_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus= ? and orderno=? and compid  = ? ";


    //查询分摊余额
    private static final String QUERY_ORDER_GOODS_BAL_SQL = "select balamt from {{?" + DSMConst.TD_TRAN_GOODS + "}}   "
            + " where cstatus&1=0 and orderno = ? and pdno = ? and compid = ? ";


    private static final String UPD_ASAPP_CK_FINISH_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus = ? "+
            " where cstatus&1=0 and asno = ?  ";



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
        boolean b = false;
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        //更新订单状态为已评价
        String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|128 "
                + " where cstatus&1=0 and orderno="+ orderNo + " and ostatus in(3,4)";
        if (baseDao.updateNativeSharding(compid, year, updSQL) > 0){
            b = IceRemoteUtil.insertApprise(json) > 0;
        }

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
        if (doApply(asAppVOS, orderNo, asType)) {
            return result.fail("该订单售后申请正在处理中！");
        }

        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        if (asType == 0 || asType == 1 || asType == 2) {
            //更新订单售后状态
            sqlList.add(UPD_ORDER_SQL);
            params.add(new Object[]{-1, orderNo});
            getUpdGoodsSql(asAppVOS, sqlList, params, orderNo, compid);
            String[] sqlNative = new String[sqlList.size()];
            sqlNative = sqlList.toArray(sqlNative);
            b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid, year, sqlNative, params));
            if (b) {
                //向售后表插入申请数据
                getInsertSql(asAppVOS, paramsInsert);
                b = !ModelUtil.updateTransEmpty(baseDao.updateBatchNativeSharding(0,localDateTime.getYear(),
                        INSERT_ASAPP_SQL, paramsInsert, asAppVOS.size()));
            }
        } else {
            //改变订单状态为已补开发票
            String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|256 "
                    + " where cstatus&1=0 and orderno=? and ostatus in(3,4)";
            if (baseDao.updateNativeSharding(compid, year, updSQL, orderNo) > 0) {
                //向售后表插入申请数据
                String asOrderId = GenIdUtil.getAsOrderId();
                Object[] pramsObj = new Object[]{asAppVOS.get(0).getOrderno(), asAppVOS.get(0).getPdno(), asOrderId,
                        asAppVOS.get(0).getCompid(), asAppVOS.get(0).getAstype(), asAppVOS.get(0).getGstatus(), asAppVOS.get(0).getReason(),
                        asAppVOS.get(0).getCkstatus(), asAppVOS.get(0).getCkdesc(), asAppVOS.get(0).getInvoice(), 1,
                        asAppVOS.get(0).getApdesc(), asAppVOS.get(0).getRefamt() * 100, asAppVOS.get(0).getAsnum()};
                int res = baseDao.updateNativeSharding(0, localDateTime.getYear(), INSERT_ASAPP_SQL, pramsObj);

                return res > 0 ? result.success(asOrderId) : result.fail("申请失败");
            }
        }
        return b ? result.success("申请成功") : result.fail("申请失败");
    }

    private boolean doApply(List<AsAppVO> asAppVOS, String orderNo, int asType) {
        String sql;
        LocalDateTime localDateTime = LocalDateTime.now();
        if (asType == 3 || asType == 4){
            sql = "select count(*) from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where cstatus&1=0 "
                    + " and orderno=" + orderNo + " and astype in(3,4)";
        } else {
            StringBuilder skuBuilder = new StringBuilder();
            for (AsAppVO asAppVO : asAppVOS) {
                skuBuilder.append(asAppVO.getPdno()).append(",");
            }
            String pdnoStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
            sql = "select count(*) from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where cstatus&1=0 "
                    + " and orderno=" + orderNo + " and ckstatus in(0,1) and astype not in(3,4) and pdno "
                    + "in(" + pdnoStr + ")";
        }
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
                    int year = Integer.parseInt("20" + orderNo.substring(0, 2));
                    List<Object[]> ret = baseDao.queryNativeSharding(compid, year, 
                            QUERY_ORDER_GOODS_BAL_SQL, new Object[]{orderNo, asAppVO.getPdno(), compid});
                    double bal = 0;
                    if(ret != null && !ret.isEmpty()){
                        bal = Double.parseDouble(ret.get(0)[0].toString());
                    }

                    bal = MathUtil.exactDiv(bal,100).setScale(2, BigDecimal.ROUND_HALF_UP).
                            doubleValue();

                    double payamt = MathUtil.exactDiv(tranOrderGoods1.getPayamt(),100).
                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double acprice = MathUtil.exactDiv(payamt, tranOrderGoods1.getPnum()).
                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    double refamt =  MathUtil.exactMul(acprice, asAppVO.getAsnum()).
                            setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                    if(bal > 0){
                        bal = MathUtil.exactDiv(bal,
                                tranOrderGoods1.getPnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

                        double acbalamt = MathUtil.exactMul(bal,
                                asAppVO.getAsnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();


                        refamt = MathUtil.exactAdd(refamt,acbalamt).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    }
                    asAppVO.setRefamt(refamt);
                }
                if (asAppVO.getPdno() == tranOrderGoods1.getPdno() && asAppVO.getAsnum() == tranOrderGoods1.getPnum()){
                    skuList.add(asAppVO.getPdno());
                }
            }
        }
        if (skuList.size() == tranOrderGoods.length) {
            for (TranOrderGoods tog : tranOrderGoods) {
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1,tog.getPdno(),0, orderNo,compid});
            }
        } else {
            for (AsAppVO asAppVO : asAppVOS) {
                long sku = asAppVO.getPdno();
                sqlList.add(UPD_ORDER_GOODS_SQL);
                params.add(new Object[]{1, sku, 0, orderNo,compid});
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
            paramsInsert.add(new Object[]{asAppVO.getOrderno(), asAppVO.getPdno(), GenIdUtil.getAsOrderId(),
                    asAppVO.getCompid(), asAppVO.getAstype(), asAppVO.getGstatus(), asAppVO.getReason(),
                    asAppVO.getCkstatus(), asAppVO.getCkdesc(), asAppVO.getInvoice(),0,
                    asAppVO.getApdesc(), asAppVO.getRefamt() * 100, asAppVO.getAsnum()});
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
                + " where cstatus&1=0 and pdno=? and orderno = ? and compid = ? and asstatus in(1,2,3)";
        int resultCode = baseDao.updateNativeSharding(compId, year, updOrderGoodsSql,
                -1, sku,orderNo,compId);

        if (resultCode > 0) {
            //更新售后表状态
            resultCode = baseDao.updateNativeSharding(0,localDateTime.getYear(),
                    UPD_ASAPP_SQL, -2, sku,orderNo,compId);

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
    @UserPermission(ignore = false,allowedUnrelated =true)
    public Result afterSaleReview(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        if(userSession == null || (userSession.roleCode & (128+1) )== 0){
            return result.fail("当前用户没有该权限");
        }


        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long asno = jsonObject.get("asno").getAsLong();
        String ckdesc = jsonObject.get("ckdesc").getAsString();
        int ckstatus =  jsonObject.get("ckstatus").getAsInt();
        int astype =  jsonObject.get("astype").getAsInt();
        double refamt = jsonObject.get("refamt").getAsDouble();

        String queryOrderno = "select orderno,compid,pdno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";

        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);

        if(queryRet == null || queryRet.isEmpty()){
            return result.fail("操作失败");
        }

        int ret = baseDao.updateNativeSharding(0,
                TimeUtils.getCurrentYear(),UPD_ASAPP_CK_SQL,
                ckstatus, userSession.userId,userSession.userName, astype, ckdesc,refamt*100,asno);

        if (ret > 0){
            //退货失败
            int ostatus = 3;
            int gstatus = 4;

            int compid = Integer.parseInt(queryRet.get(0)[1].toString());
            String specifyStorePhone = IceRemoteUtil.getSpecifyStorePhone(compid);


            if(ckstatus == 1){
                //1退款退货 2 仅退款
                if(astype == 1 || astype == 2){
                    List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                            TimeUtils.getCurrentYear(), QUERY_ASAPP_INFO_SQL, asno);
                    int asnum = Integer.parseInt(queryResult.get(0)[4].toString());
                    double balamt = Double.parseDouble(queryResult.get(0)[21].toString());
                    int pnum = Integer.parseInt(queryResult.get(0)[8].toString());
                    LogUtil.getDefaultLogger().debug("审核通过获取余额："+balamt);
                    if(balamt > 0){
                        double apamt = MathUtil.exactDiv(balamt,pnum).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        LogUtil.getDefaultLogger().debug("审核通过单个商品分摊余额："+apamt+" 商品数量："+pnum);

                        double subbal = MathUtil.exactMul(asnum,apamt).
                                setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        LogUtil.getDefaultLogger().debug("实际退回余额分摊余额："+subbal+" 退货数量："+asnum);
                        IceRemoteUtil.updateCompBal(Integer.parseInt(queryRet.get(0)[1].toString()),MathUtil.exactMul(subbal,100).intValue());
                    }
                    SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_AUDIT_PASSED,asno+"");
                }
                ostatus = -2;
                gstatus = 3;

                if(astype == 3 || astype == 4){
                    SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_BILL_AUDIT_PASSED,asno+"");
                }


            }else{
                if(astype == 1 || astype == 2){
                    SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_AUDIT_FAILED_TO_PASSED,asno+"",ckdesc);
                }

                if(astype == 3 || astype == 4){
                    SmsTempNo.sendMessageToSpecify(compid,specifyStorePhone,
                            SmsTempNo.AFTER_SALE_BILL_AUDIT_FAILED_TO_PASSED,asno+"",ckdesc);
                }
            }
            int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{ostatus, queryRet.get(0)[0],-1});
            params.add(new Object[]{gstatus, queryRet.get(0)[2],1,queryRet.get(0)[0],queryRet.get(0)[1]});
            baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()),year,
            new String[]{UPD_ORDER_CK_SQL,UPD_ORDER_GOODS_CK_SQL},params);
        }

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

    /* *
     * @description 查询售后订单详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/24 11:04
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryAsOrderInfo(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String asno = jsonObject.get("asno").getAsString();

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), QUERY_ASAPP_INFO_SQL, asno);


        if (queryResult.isEmpty()) {
            return null;
        }

        AsAppDtVO[] asAppDtVOs = new AsAppDtVO[queryResult.size()];

        baseDao.convToEntity(queryResult, asAppDtVOs, AsAppDtVO.class,
                new String[]{"orderno", "compid", "asno", "pdno", "asnum",
                        "spdprice", "spayamt", "sdistprice", "pnum",
                        "astype", "reason", "apdesc", "refamt", "ckstatus", "ckdesc", "gstatus",
                        "ckdate", "cktime", "apdata", "aptime", "cstatus","balamt"});

        AsAppDtVO asAppDtVO = asAppDtVOs[0];
        if (asAppDtVO != null) {
            double acprice = MathUtil.exactDiv(asAppDtVO.getSpayamt(), asAppDtVO.getPnum()).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            double disprice = MathUtil.exactDiv(asAppDtVO.getDistprice(), asAppDtVO.getPnum()).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();

            asAppDtVO.setDistprice(disprice);
            asAppDtVO.setAcprice(acprice);

//            MathUtil.exactMul(asAppDtVO.getAsnum(),asAppDtVO.getBalamt()).
//                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()
            asAppDtVO.setPayamt(MathUtil.exactMul(asAppDtVO.getAsnum(),acprice)
                    .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());


           double acbalamt = MathUtil.exactDiv(asAppDtVO.getBalamt(),
                    asAppDtVO.getPnum()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();


            asAppDtVO.setRefbal(MathUtil.exactMul(asAppDtVO.getAsnum(),acbalamt).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

            asAppDtVO.setSumRefAmt(asAppDtVO.getRefamt());
            asAppDtVO.setBalamt(acbalamt);
            ProdEntity prodBySku = null;
            try {
                prodBySku = ProdInfoStore.getProdBySku(asAppDtVO.getPdno());
                if (prodBySku != null) {
                    asAppDtVO.setSpec(prodBySku.getSpec());
                    asAppDtVO.setPname(prodBySku.getProdname());
                    asAppDtVO.setManuName(prodBySku.getManuName());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result.success(asAppDtVO);
    }

    /* *
     * @description 查询售后订单详情[补开发票]
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author jiangwenguang
     * @time  2019/4/29 17:44
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryAsOrderInvoiceInfo(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String asno = jsonObject.get("asno").getAsString();

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), QUERY_ASAPP_INVOICE_INFO_SQL, asno);


        if (queryResult.isEmpty()) {
            return null;
        }

        AsAppDtVO[] asAppDtVOs = new AsAppDtVO[queryResult.size()];
        baseDao.convToEntity(queryResult, asAppDtVOs, AsAppDtVO.class,
                new String[]{"orderno", "compid", "asno", "pdno", "asnum",
                        "astype", "reason", "apdesc", "refamt", "ckstatus", "ckdesc", "gstatus",
                        "ckdate", "cktime", "apdata", "aptime", "cstatus", "invoice"});

        JSONObject resultObject = new JSONObject();
        AsAppDtVO asAppDtVO = asAppDtVOs[0];
        if (asAppDtVO != null) {
            asAppDtVO.setPaytype(2);
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtVO.getCompid()));

            JSONObject compJson = JSON.parseObject(compStr);

            if (compJson != null) {
                asAppDtVO.setAreaAllName(
                        IceRemoteUtil.getCompleteName(compJson.getString("addressCode")));
            }
            resultObject.put("asapp", asAppDtVO);
            queryResult = baseDao.queryNativeSharding(0,
                    TimeUtils.getCurrentYear(), QUERY_GOODS_SQL, asAppDtVO.getOrderno(), asAppDtVO.getCompid());
            List<TranOrderGoods> list = new ArrayList<>();
            if(queryResult != null && queryResult.size() > 0){
                for(Object[] arr : queryResult){
                    TranOrderGoods goods = new TranOrderGoods();
                    goods.setPdno(Long.parseLong(arr[0].toString()));
                    ProdEntity prodBySku = null;
                    try {
                        prodBySku = ProdInfoStore.getProdBySku(goods.getPdno());

                        if (prodBySku != null) {
                            goods.setPname(prodBySku.getProdname());
                            goods.setPspec(prodBySku.getSpec());
                            goods.setManun(prodBySku.getManuName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    goods.setPnum(Integer.parseInt(arr[1].toString()));
                    goods.setPdprice(MathUtil.exactDiv(Double.parseDouble(arr[2].toString()), 100).doubleValue());
                    goods.setDistprice(MathUtil.exactDiv(Double.parseDouble(arr[3].toString()), 100).doubleValue());
                    goods.setPayamt(MathUtil.exactDiv(Double.parseDouble(arr[4].toString()), 100).doubleValue());
                    list.add(goods);
                }
            }
            resultObject.put("goodslist", list);

        }
        return result.success(resultObject);
    }


    /* *
     * @description 查询售后订单列表
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/24 11:04
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryAsOrderList(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_ASAPP_LIST_SQL);

        int astype = jsonObject.get("astype").getAsInt();

        String sdate = jsonObject.get("sdate").getAsString();

        String edate = jsonObject.get("edate").getAsString();

        int ckstatus =jsonObject.get("ckstatus").getAsInt();

        if(astype > -1){
            sqlBuilder.append(" and asapp.astype = ");
            sqlBuilder.append(astype);
        }

        if(ckstatus != -2){
            sqlBuilder.append(" and asapp.ckstatus = ");
            sqlBuilder.append(ckstatus);
        }


        if(!StringUtils.isEmpty(sdate) && StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata >= '").append(sdate).append("' ");
        }

        if(StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata <= '").append(edate).append("' ");
        }

        if(!StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata between '").append(sdate).append("' and '").append(edate).append("' ");
        }
        sqlBuilder.append(" order by asapp.apdata,asapp.aptime desc ");

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), pageHolder, page,sqlBuilder.toString());


        AsAppDtListVO[] asAppDtListVOS = new AsAppDtListVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(asAppDtListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, asAppDtListVOS, AsAppDtListVO.class,
                new String[]{"orderno", "compid", "asno", "astype", "ckstatus",
                        "gstatus", "apdata", "aptime", "checkern",
                        "contact", "address","refamt","compn"});

        for(AsAppDtListVO asAppDtListVO: asAppDtListVOS){
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtListVO.getCompid()));
            if(!StringUtils.isEmpty(compStr)){
                JSONObject compJson = JSON.parseObject(compStr);
                asAppDtListVO.setCompn(compJson.getString("storeName"));
            }
        }

        return result.setQuery(asAppDtListVOS, pageHolder);
    }



    /* *
     * @description 查询售后订单发票列表
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/24 11:04
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryAsOrderBillList(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int pageSize = jsonObject.get("pageSize").getAsInt();
        int pageIndex = jsonObject.get("pageNo").getAsInt();
        Page page = new Page();
        page.pageSize = pageSize;
        page.pageIndex = pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        Result result = new Result();


        StringBuilder sqlBuilder = new StringBuilder(QUERY_ASAPP_BILL_LIST_SQL);

        int astype = jsonObject.get("astype").getAsInt();

        String sdate = jsonObject.get("sdate").getAsString();

        String edate = jsonObject.get("edate").getAsString();

        int ckstatus =jsonObject.get("ckstatus").getAsInt();

        if(astype > -1){
            sqlBuilder.append(" and asapp.astype = ");
            sqlBuilder.append(astype);
        }

        if(ckstatus != -2){
            sqlBuilder.append(" and asapp.ckstatus = ");
            sqlBuilder.append(ckstatus);
        }


        if(!StringUtils.isEmpty(sdate) && StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata >= '").append(sdate).append("' ");
        }

        if(StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata <= '").append(edate).append("' ");
        }

        if(!StringUtils.isEmpty(sdate) && !StringUtils.isEmpty(edate)){
            sqlBuilder.append(" and apdata between '").append(sdate).append("' and '").append(edate).append("' ");
        }

        sqlBuilder.append(" order by asapp.apdata,asapp.aptime desc ");

        List<Object[]> queryResult = baseDao.queryNativeSharding(0,
                TimeUtils.getCurrentYear(), pageHolder, page,sqlBuilder.toString());



        AsAppDtListVO[] asAppDtListVOS = new AsAppDtListVO[queryResult.size()];
        if (queryResult == null || queryResult.isEmpty()) {
            return result.setQuery(asAppDtListVOS, pageHolder);
        }

        baseDao.convToEntity(queryResult, asAppDtListVOS, AsAppDtListVO.class,
                new String[]{"orderno", "compid", "asno", "astype", "ckstatus",
                        "gstatus", "apdata", "aptime", "checkern",
                        "contact", "address","compn"});

        for(AsAppDtListVO asAppDtListVO: asAppDtListVOS){
            String compStr = RedisUtil.getStringProvide()
                    .get(String.valueOf(asAppDtListVO.getCompid()));
            if(!StringUtils.isEmpty(compStr)){
                JSONObject compJson = JSON.parseObject(compStr);
                asAppDtListVO.setCompn(compJson.getString("storeName"));
            }
        }


        return result.setQuery(asAppDtListVOS, pageHolder);
    }

    /**
     * 根据活动起始时间和结束时间和活动码查询团购订单记录
     * @param appContext
     * @return
     */
    @UserPermission(ignore = true)
    public Result queryTeamBuyOrder(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String sdate = jsonObject.get("sdate").getAsString();
        String edate = jsonObject.get("edate").getAsString();
        String actno = jsonObject.get("actno").getAsString();

        JSONArray jsonArray = new JSONArray();
        List<Object[]> queryList = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), QUERY_TEAM_BUY_ORDER_SQL, new Object[]{sdate, edate, actno});
        if(queryList != null && queryList.size() > 0){
            for(Object [] obj : queryList){
                JSONObject j = new JSONObject();
                j.put("orderno", obj[0].toString());
                j.put("payamt", obj[1].toString());
                j.put("compid", obj[2].toString());
                j.put("pnum", obj[3].toString());
                jsonArray.add(j);
            }
        }
        return new Result().success(jsonArray);
    }


    /* *
     * @description 确认签收
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/11 16:18
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result confirmReceipt(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderNo = jsonObject.get("orderno").getAsString();//订单号
        int cusno = jsonObject.get("cusno").getAsInt(); //企业码
        if (comReceipt(orderNo, cusno)) {
            //移出队列
            CONFIRM_RECEIPT.removeByKey(orderNo);
            return result.success("操作成功");
        }
        return result.fail("操作失败");
    }

    public boolean comReceipt(String orderNo,int cusno ) {
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        //更新订单状态为交易完成
        String updSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=4 "
                + " where cstatus&1=0 and orderno="+ orderNo + " and ostatus=3";
       return baseDao.updateNativeSharding(cusno, year, updSQL) > 0;

    }


    /* *
     * @description 售后完成更新状态
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @version 1.1.1
     **/
    @UserPermission(ignore = false,allowedUnrelated =true)
    public Result afterSaleFinish(AppContext appContext) {
        String json = appContext.param.json;
        Result result = new Result();
        JsonParser jsonParser = new JsonParser();
        UserSession userSession = appContext.getUserSession();
        if(userSession == null || (userSession.roleCode & (128+1) )== 0){
            return result.fail("当前用户没有该权限");
        }

        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long asno = jsonObject.get("asno").getAsLong();
        String queryOrderno = "select orderno,compid,pdno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";
        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);
        if(queryRet == null || queryRet.isEmpty()){
            return result.fail("操作失败");
        }

            int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));
            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{-3, queryRet.get(0)[0],-2});
            params.add(new Object[]{200, queryRet.get(0)[2],3,queryRet.get(0)[0],queryRet.get(0)[1]});


            baseDao.updateNativeSharding(0,TimeUtils.getCurrentYear(),UPD_ASAPP_CK_FINISH_SQL,new Object[]{200,asno});
            boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()),year,
                    new String[]{UPD_ORDER_CK_SQL,UPD_ORDER_GOODS_CK_SQL},params));

        return b ? result.success("操作成功") : result.fail("操作失败");
    }
}
