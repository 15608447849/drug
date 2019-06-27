package com.onek.erp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entitys.Result;
import com.onek.erp.entities.ERPGoodsVO;
import com.onek.prop.AppProperties;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.TimeUtils;
import util.http.HttpRequestUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * @服务名 orderServer
 * @author 11842
 * @version 1.1.1
 * @description ERP订单对接处理(异步执行)
 * @time 2019/6/26 10:40
 **/
public class OrderDockedWithERP {

    private static AppProperties appProperties = AppProperties.INSTANCE;

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();


    public static void generationOrder2ERP(String orderNo, int compId) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Object future = executorService.submit((Callable<Object>) () -> {
                if (systemConfigOpen("ORDER_SYNC")) {
                    return generateOrder(orderNo, compId, 0);
                } else {
                    LogUtil.getDefaultLogger().info("订单order信息同步开关未开启>>>>>>>>>>>>>>>");
                }
                return -1;
            }).get();
            if (future != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5 * 1000, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static boolean systemConfigOpen(String name) {
        String selectSQL = "select count(*) from {{?" + DSMConst.TB_SYSTEM_CONFIG + "}} where "
                + " cstatus&1=0 and value=1 and varname=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, name);
        return Long.valueOf(String.valueOf(queryResult.get(0)[0])) > 0;
    }

    private static int generateOrder(String orderNo, int compId, int type) {
        JsonObject orderObj = new JsonObject();
        String selectOrderSQL = "select orderno,cusno,payamt,remarks,invoicetype,rvaddno,address,"
                + " consignee,contact from {{?" + DSMConst.TD_TRAN_ORDER + "}} where cstatsu&1=0 and "
                + " orderno=?";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectOrderSQL, orderNo);
        TranOrder[] tranOrders = new TranOrder[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrders, TranOrder.class, new String[]{
                "orderno", "cusno", "payamt", "remarks", "invoicetype", "rvaddno",
                "address", "consignee", "contact"
        });
        List<ERPGoodsVO> oGoodsList = getOrderGoods(orderNo, compId);
        List<ERPGoodsVO> oGiftList = getOrderGift(orderNo, compId);
        if (oGiftList != null) {
            oGoodsList.addAll(oGiftList);
        }
        orderObj.addProperty("orderno", tranOrders[0].getOrderno());
        orderObj.addProperty("compid", tranOrders[0].getCusno());
        orderObj.addProperty("payamt", tranOrders[0].getPayamt());
        orderObj.addProperty("remarks", tranOrders[0].getRemarks());
        orderObj.addProperty("invoicetype", tranOrders[0].getInvoicetype());
        String completeName = IceRemoteUtil.getCompleteName(tranOrders[0].getRvaddno() + "") + tranOrders[0].getAddress();
        orderObj.addProperty("address", completeName);
        orderObj.addProperty("consignee", tranOrders[0].getConsignee());
        orderObj.addProperty("contact", tranOrders[0].getContact());
        orderObj.addProperty("detail", new Gson().toJson(oGoodsList));
        return postOrder2ERP(orderObj, type, orderNo, compId);
    }

    private static int postOrder2ERP(JsonObject jsonObject, int type, String orderNo, int compId) {
        try {
            String url = appProperties.erpUrlPrev + "/produceSalesOrder";
            String result = HttpRequestUtil.postJson(url, jsonObject.toString());
            LogUtil.getDefaultLogger().info("调用ERP接口结果返回： " + result);
            if (result != null && !result.isEmpty()) {
                int code = new JsonParser().parse(result).getAsJsonObject().get("code").getAsInt();
                if (code != 200 && type == 0) {//同步失败处理
                    updateOrderState(orderNo, compId);
                } else if (code == 200 && type == 1) {
                    updateOState(orderNo, compId);
                }
                return code;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /* *
     * @description 订单同步到ERP失败处理
     * @params [compid]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/26 14:31
     * @version 1.1.1
     **/
    private static void updateOrderState(String orderNo, int compId) {
        String updOrderSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus|4096 where "
                + " cstatus&1=0 and orderno=?";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        baseDao.updateNativeSharding(compId, year, updOrderSQL, orderNo);
    }

    private static void updateOState(String orderNo, int compId) {
        String updOrderSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set cstatus=cstatus&~4096 where "
                + " cstatus&1=0 and orderno=?";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        baseDao.updateNativeSharding(compId, year, updOrderSQL, orderNo);
    }

    private static List<ERPGoodsVO> getOrderGift(String orderNo, int compId) {
        String selectRebSQL = "select unqid,rebate from {{?" + DSMConst.TD_TRAN_REBATE + "}} where "
                + " cstatus&1=0 and orderno=? and JSON_CONTAINS(rebate->'$[*].type', '3', '$') ";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectRebSQL, orderNo);
        if (queryResult == null || queryResult.isEmpty()) return null;
        List<ERPGoodsVO> erpGoodsVOList = new ArrayList<>();
        queryResult.forEach(qResult -> {
            JsonArray rebateArr = new JsonParser().parse(String.valueOf(qResult[1])).getAsJsonArray();
            for (int i = 0; i < rebateArr.size(); i++) {
                JsonObject jsonObject = rebateArr.get(i).getAsJsonObject();
                ERPGoodsVO erpGoodsVO = new ERPGoodsVO();
                erpGoodsVO.setUnqid(String.valueOf(qResult[0]));
                erpGoodsVO.setErpsku(jsonObject.get("id").getAsString());
                erpGoodsVO.setNum(jsonObject.get("nums").getAsString());
                erpGoodsVO.setPayamt("0");
                erpGoodsVO.setPdprice("0");
                erpGoodsVOList.add(erpGoodsVO);
            }

        });
        return erpGoodsVOList;
    }

    private static List<ERPGoodsVO> getOrderGoods(String orderNo, int compId) {
        String selectGoodsSQL = "select unqid,pdno,pnum,pdprice,payamt from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where cstatus&1=0 and orderno=? ";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectGoodsSQL, orderNo);
        ERPGoodsVO[] erpGoodsVOS = new ERPGoodsVO[queryResult.size()];
        baseDao.convToEntity(queryResult, erpGoodsVOS, ERPGoodsVO.class, new String[]{
                "unqid", "erpsku", "num", "pdprice", "payamt"
        });
        return Arrays.asList(erpGoodsVOS);
    }

    /**
     * @接口摘要 同步订单信息异常处理接口
     * @业务场景 同步ERP异常
     * @传参类型
     * @参数列表
     * @返回列表 200成功
     */
    @UserPermission(ignore = true)
    public Result syncOrderErpDtOnLine(AppContext appContext) {
        Result result = new Result();
        String selectCompSQL = "select orderno,cusno from {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} where cstatus&1=0 and "
                + " cstatus&4096>0";
        List<Object[]> queryResult = baseDao.queryNativeSharding(0,TimeUtils.getCurrentYear(),selectCompSQL);
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Object future = executorService.submit(() -> {
                if (queryResult != null && queryResult.size() > 0) {
                    for (Object[] objects:queryResult) {
                        generateOrder(String.valueOf(objects[0]),(int)objects[1], 1);
                    }
                }
                return 0;
            }).get();
            if (future != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5*1000, TimeUnit.MILLISECONDS)){
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return result.fail("操作失败");
        }
        return result.success();

    }

}
