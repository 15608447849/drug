package com.onek.order;

import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.SyncErrVO;
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

import java.util.*;
import java.util.concurrent.*;

/**
 * @服务名 orderServer
 * @author 11842
 * @version 1.1.1
 * @description ERP订单对接处理(异步执行)
 * @time 2019/6/26 10:40
 **/
public class OrderDockedWithERPModule {

    private static AppProperties appProperties = AppProperties.INSTANCE;

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();


    public static void generationOrder2ERP(String orderNo, int compId) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            Object future = executorService.submit((Callable<Object>) () -> {
                if (IceRemoteUtil.systemConfigOpen("ORDER_SYNC")) {
                    return generateOrder(orderNo, 0, compId);
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


    private static int generateOrder(String orderNo, int type, int compId) {
        String selectOrderSQL = "select orderno,cusno,payamt,remarks,invoicetype,rvaddno,address,"
                + " consignee,contact,freight from {{?" + DSMConst.TD_TRAN_ORDER + "}} where cstatus&1=0 and "
                + " orderno=?";
        int year = TimeUtils.getYearByOrderno(orderNo + "");
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectOrderSQL, orderNo);
        TranOrder[] tranOrders = new TranOrder[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrders, TranOrder.class, "orderno", "cusno", "payamt", "remarks", "invoicetype", "rvaddno",
                "address", "consignee", "contact", "freight");
        List<ERPGoodsVO> oGoodsList = getOrderGoodsPO(orderNo, compId);
        List<ERPGoodsVO> oGiftList = getOrderGiftPO(orderNo, compId);
        if (oGiftList != null) {
            oGoodsList.addAll(oGiftList);
        }
        tranOrders[0].setErpGoodsVOS(oGoodsList);
        return postOrder2ERP(combatData(tranOrders[0]), type, orderNo);
    }

    private static int postOrder2ERP(JsonObject jsonObject, int type, String orderNo) {
        try {
            String url = appProperties.erpUrlPrev + "/produceSalesOrder";
            LogUtil.getDefaultLogger().info("生成订单调用ERP接口参数： " + jsonObject.toString());
            String result = HttpRequestUtil.postJson(url, jsonObject.toString());
            LogUtil.getDefaultLogger().info("生成订单调用ERP接口结果返回： " + result);
            if (result != null && !result.isEmpty()) {
                JsonObject object = new JsonParser().parse(result).getAsJsonObject();
                int code = object.get("code").getAsInt();
                if (code != 200 && type == 0) {//同步失败处理
                    int errorCode = object.get("errorcode").getAsInt();
                    updateOrderState(orderNo, errorCode);
                } else if (code == 200 && type == 1) {
                    updateOState(orderNo);
                }
                return code;
            }
        } catch (Exception e) {
            if (type == 0) {
                updateOrderState(orderNo, 1);
            }
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
    private static void updateOrderState(String orderNo, int errorCode) {
        SyncErrVO syncErrVO = new SyncErrVO();
        syncErrVO.setSyncfrom(1);
        syncErrVO.setSyncreason(errorCode);
        syncErrVO.setSynctype(2);
        syncErrVO.setSyncid(Long.parseLong(orderNo));
        syncErrVO.setSyncway(1);
        IceRemoteUtil.insertOrUpdSyncErr(syncErrVO);
    }

    private static void updateOState(String orderNo) {
        IceRemoteUtil.updSyncCState(Long.parseLong(orderNo));
    }

    private static List<ERPGoodsVO> getOrderGift(String orderNoStr) {
        String selectRebSQL = "select unqid,rebate,orderno from {{?" + DSMConst.TD_BK_TRAN_REBATE + "}} where "
                + " cstatus&1=0 and orderno in (" + orderNoStr
                + ") and JSON_CONTAINS(rebate->'$[*].type', '3', '$') ";
        int year = orderNoStr.contains(",") ? TimeUtils.getCurrentYear() : TimeUtils.getYearByOrderno(orderNoStr);
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, year, selectRebSQL);
        if (queryResult == null || queryResult.isEmpty()) return null;
        List<ERPGoodsVO> erpGoodsVOList = new ArrayList<>();
        queryResult.forEach(qResult -> {
            JsonArray rebateArr = new JsonParser().parse(String.valueOf(qResult[1])).getAsJsonArray();
            for (int i = 0; i < rebateArr.size(); i++) {
                JsonObject jsonObject = rebateArr.get(i).getAsJsonObject();
                if (jsonObject.has("type") && jsonObject.get("type").getAsInt() == 3) {
                    ERPGoodsVO erpGoodsVO = new ERPGoodsVO();
                    erpGoodsVO.setUnqid(String.valueOf(qResult[0]));
                    erpGoodsVO.setErpsku(jsonObject.get("id").getAsString());
                    erpGoodsVO.setNum(jsonObject.get("totalNums").getAsString());
                    erpGoodsVO.setPayamt("0");
                    erpGoodsVO.setPdprice("0");
                    erpGoodsVO.setOrderno(String.valueOf(qResult[2]));
                    erpGoodsVOList.add(erpGoodsVO);
                }
            }

        });
        return erpGoodsVOList;
    }

    private static List<ERPGoodsVO> getOrderGiftPO(String orderNo, int compId) {
        String selectRebSQL = "select unqid,rebate,orderno from {{?" + DSMConst.TD_TRAN_REBATE + "}} where "
                + " cstatus&1=0 and orderno="+ orderNo
                + " and JSON_CONTAINS(rebate->'$[*].type', '3', '$') ";
        int year = TimeUtils.getYearByOrderno(orderNo);
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectRebSQL);
        if (queryResult == null || queryResult.isEmpty()) return null;
        List<ERPGoodsVO> erpGoodsVOList = new ArrayList<>();
        queryResult.forEach(qResult -> {
            JsonArray rebateArr = new JsonParser().parse(String.valueOf(qResult[1])).getAsJsonArray();
            for (int i = 0; i < rebateArr.size(); i++) {
                JsonObject jsonObject = rebateArr.get(i).getAsJsonObject();
                if (jsonObject.has("type") && jsonObject.get("type").getAsInt() == 3) {
                    ERPGoodsVO erpGoodsVO = new ERPGoodsVO();
                    erpGoodsVO.setUnqid(String.valueOf(qResult[0]));
                    erpGoodsVO.setErpsku(jsonObject.get("id").getAsString());
                    erpGoodsVO.setNum(jsonObject.get("totalNums").getAsString());
                    erpGoodsVO.setPayamt("0");
                    erpGoodsVO.setPdprice("0");
                    erpGoodsVO.setOrderno(String.valueOf(qResult[2]));
                    erpGoodsVOList.add(erpGoodsVO);
                }
            }

        });
        return erpGoodsVOList;
    }

    private static List<ERPGoodsVO> getOrderGoodsPO(String orderNo, int compId) {
        String selectGoodsSQL = "select orderno,unqid,pdno,pnum,pdprice,payamt from {{?" + DSMConst.TD_TRAN_GOODS + "}} "
                + " where cstatus&1=0 and orderno=" + orderNo;
        int year = TimeUtils.getYearByOrderno(orderNo);
        List<Object[]> queryResult = baseDao.queryNativeSharding(compId, year, selectGoodsSQL);
        ERPGoodsVO[] erpGoodsVOS = new ERPGoodsVO[queryResult.size()];
        baseDao.convToEntity(queryResult, erpGoodsVOS, ERPGoodsVO.class,
                "orderno","unqid", "erpsku", "num", "pdprice", "payamt");
        List<ERPGoodsVO> erpGoodsVOList = new ArrayList<>(Arrays.asList(erpGoodsVOS));
        StringBuilder skuBuilder = new StringBuilder();
        erpGoodsVOList.forEach(erpGoodsVO -> {
            skuBuilder.append(erpGoodsVO.getErpsku()).append(",");
        });
        String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
        String result = IceRemoteUtil.getErpSkuBySku(skuStr);
        if (result != null) {
            Map<String, String> erpSkuMap = new HashMap<>();
            JsonArray dataArr = new JsonParser().parse(result).getAsJsonObject().get("data").getAsJsonArray();
            for (JsonElement data : dataArr) {
                String skuS = data.getAsJsonObject().get("sku").getAsString();
                if (!erpSkuMap.containsKey(skuS)) {
                    erpSkuMap.put(skuS, data.getAsJsonObject().get("erpSku").getAsString());
                }
            }
            erpGoodsVOList.forEach(erpGoodsVO -> {
                if (erpSkuMap.containsKey(erpGoodsVO.getErpsku())) {
                    erpGoodsVO.setErpsku(erpSkuMap.get(erpGoodsVO.getErpsku()));
                }
            });
        }

        return erpGoodsVOList;
    }

    private static List<ERPGoodsVO> getOrderGoods(String orderNoStr) {
        String selectGoodsSQL = "select orderno,unqid,pdno,pnum,pdprice,payamt from {{?" + DSMConst.TD_BK_TRAN_GOODS + "}} "
                + " where cstatus&1=0 and orderno in(" + orderNoStr + ") ";
        int year = orderNoStr.contains(",") ? TimeUtils.getCurrentYear() : TimeUtils.getYearByOrderno(orderNoStr);
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, year, selectGoodsSQL);
        ERPGoodsVO[] erpGoodsVOS = new ERPGoodsVO[queryResult.size()];
        baseDao.convToEntity(queryResult, erpGoodsVOS, ERPGoodsVO.class,
                "orderno","unqid", "erpsku", "num", "pdprice", "payamt");
        List<ERPGoodsVO> erpGoodsVOList = new ArrayList<>(Arrays.asList(erpGoodsVOS));
        StringBuilder skuBuilder = new StringBuilder();
        erpGoodsVOList.forEach(erpGoodsVO -> {
            skuBuilder.append(erpGoodsVO.getErpsku()).append(",");
        });
        String skuStr = skuBuilder.toString().substring(0, skuBuilder.toString().length() - 1);
        String result = IceRemoteUtil.getErpSkuBySku(skuStr);
        if (result != null) {
            Map<String, String> erpSkuMap = new HashMap<>();
            JsonArray dataArr = new JsonParser().parse(result).getAsJsonObject().get("data").getAsJsonArray();
            for (JsonElement data : dataArr) {
                String skuS = data.getAsJsonObject().get("sku").getAsString();
                if (!erpSkuMap.containsKey(skuS)) {
                    erpSkuMap.put(skuS, data.getAsJsonObject().get("erpSku").getAsString());
                }
            }
            erpGoodsVOList.forEach(erpGoodsVO -> {
                if (erpSkuMap.containsKey(erpGoodsVO.getErpsku())) {
                    erpGoodsVO.setErpsku(erpSkuMap.get(erpGoodsVO.getErpsku()));
                }
            });
        }

        return erpGoodsVOList;
    }

    @UserPermission(ignore = true)
    public String batchProduceSalesOrder(AppContext context) {
        String orderNoStr = context.param.arrays[0];
        String selectOrderSQL = "select orderno,cusno,payamt,remarks,invoicetype,rvaddno,address,"
                + " consignee,contact,freight from {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} where cstatus&1=0 and "
                + " orderno in(" +orderNoStr+ ")";
        List<Object[]> queryResult = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), selectOrderSQL);
        TranOrder[] tranOrders = new TranOrder[queryResult.size()];
        baseDao.convToEntity(queryResult, tranOrders, TranOrder.class, "orderno", "cusno", "payamt", "remarks", "invoicetype", "rvaddno",
                "address", "consignee", "contact","freight");
        List<TranOrder> tranOrderList = new ArrayList<>(Arrays.asList(tranOrders));
        JsonObject allOrderObj = new JsonObject();
        allOrderObj.add("list", setBatchGoods(orderNoStr, tranOrderList));
        return allOrderObj.toString();
    }

    private JsonArray setBatchGoods(String orderNoStr, List<TranOrder> tranOrderList) {
        JsonArray orderArr = new JsonArray();
        HashMap<String, List<ERPGoodsVO>> erpGoodsMap = new HashMap<>();
        List<ERPGoodsVO> oGoodsList = getOrderGoods(orderNoStr);
        List<ERPGoodsVO> oGiftList = getOrderGift(orderNoStr);
        if (oGiftList != null) {
            oGoodsList.addAll(oGiftList);
        }
        for (ERPGoodsVO erpGoodsVO : oGoodsList) {
            if (erpGoodsMap.containsKey(erpGoodsVO.getOrderno())) {
                erpGoodsMap.get(erpGoodsVO.getOrderno()).add(erpGoodsVO);
            } else {
                erpGoodsMap.put(erpGoodsVO.getOrderno(), new ArrayList<>(Collections.singletonList(erpGoodsVO)));
            }
        }
        for (TranOrder tranOrder : tranOrderList) {
            if (erpGoodsMap.containsKey(tranOrder.getOrderno())) {
                tranOrder.setErpGoodsVOS(erpGoodsMap.get(tranOrder.getOrderno()));
            }
            orderArr.add(combatData(tranOrder));
        }
        return orderArr;
    }

    private static JsonObject combatData(TranOrder tranOrder) {
        JsonObject orderObj = new JsonObject();
        orderObj.addProperty("orderno", tranOrder.getOrderno());
        orderObj.addProperty("compid", tranOrder.getCusno());
        orderObj.addProperty("payamt", (tranOrder.getPayamt()-tranOrder.getFreight()) > 0
                 ? tranOrder.getPayamt()-tranOrder.getFreight() : tranOrder.getPayamt());
        orderObj.addProperty("remarks", tranOrder.getRemarks());
        orderObj.addProperty("invoicetype", tranOrder.getInvoicetype());
        String completeName = IceRemoteUtil.getCompleteName(tranOrder.getRvaddno() + "") + tranOrder.getAddress();
        orderObj.addProperty("address", completeName);
        orderObj.addProperty("consignee", tranOrder.getConsignee());
        orderObj.addProperty("contact", tranOrder.getContact());
        orderObj.addProperty("detail", new Gson().toJson(tranOrder.getErpGoodsVOS()));
        return orderObj;
    }
}
