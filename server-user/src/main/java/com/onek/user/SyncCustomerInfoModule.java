package com.onek.user;

import com.google.gson.*;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entity.SyncErrVO;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.user.entity.AptitudeVO;
import com.onek.user.entity.CompInfoVO;
import com.onek.user.entity.ConsigneeVO;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import com.onek.util.SmsUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.GsonUtils;
import util.ModelUtil;
import util.http.HttpRequestUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/* *
 * @服务名 userServer
 * @author 11842
 * @version 1.1.1
 * @description 客户信息同步到ERP
 * @time 2019/6/24 10:01
 **/
public class SyncCustomerInfoModule {
    private static AppProperties appProperties = AppProperties.INSTANCE;

    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String INSERT_SYNC_SQL = "insert into {{?" + DSMConst.TD_SYNC_ERROR + "}} "
            + " (unqid,synctype,syncid,syncmsg,cstatus,"
            + "syncdate,synctime,syncfrom,syncreason,synctimes,"
            + "syncway)  values(?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?,?,?,?)";
    private static final String UPDATE_SYNC_SQL = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set syncdate=CURRENT_DATE,"
            + " synctime=CURRENT_TIME,syncreason=?,synctimes=synctimes+1 where cstatus&1=0 and "
            + " unqid=? ";

    private static JsonObject combatData(CompInfoVO compInfoVO) {
        int compId = compInfoVO.getCid();
        if (compId > 0) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("compid", compId);
            jsonObject.addProperty("cname", compInfoVO.getCname());
            jsonObject.addProperty("caddrcode", compInfoVO.getCaddrcode());
            jsonObject.addProperty("taxpayer", compInfoVO.getTaxpayer());
            jsonObject.addProperty("bankers", compInfoVO.getBankers());
            jsonObject.addProperty("account", compInfoVO.getAccount());
            jsonObject.addProperty("corporation", "");
            jsonObject.addProperty("caddr", compInfoVO.getCaddr());
            jsonObject.addProperty("storetype", compInfoVO.getStoretype());
            jsonObject.addProperty("staddr", "");
            jsonObject.addProperty("contact", compInfoVO.getContactname());
            jsonObject.addProperty("tel", compInfoVO.getContactphone());
            jsonObject.addProperty("principal", "");
            jsonObject.addProperty("qcprincipal", "");
            jsonObject.addProperty("email", compInfoVO.getEmail());
            jsonObject.addProperty("aptlist", new Gson().toJson(compInfoVO.getAptitudeVOS()));
            return jsonObject;
        }
        return null;


    }

    /* *
     * @description 异步处理---->>>> 同步企业信息到ERP 若失败进行异常处理
     * @params [compInfoVO]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/24 14:12
     * @version 1.1.1
     **/
    public static void addOrUpdateCus(CompInfoVO compInfoVO) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Object future = executorService.submit((Callable<Object>) () -> {
                //客户信息同步开关是否开启
                if (systemConfigOpen("CUS_SYNC")) {
                    return postAddOrUpdateCus(compInfoVO);
                } else {
                    LogUtil.getDefaultLogger().info("客户信息同步开关未开启>>>>>>>>>>>>>>>");
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

    private static long postAddOrUpdateCus(CompInfoVO compInfoVO) {
        try {
            ConsigneeVO consigneeVO = MyDrugStoreInfoModule.getDefaultConsignee(compInfoVO.getCid());
            compInfoVO.setContactname(consigneeVO.getContactname());
            compInfoVO.setContactphone(consigneeVO.getContactphone());
            compInfoVO.setAccount(compInfoVO.getInvoiceVO().getAccount());
            compInfoVO.setBankers(compInfoVO.getInvoiceVO().getBankers());
            compInfoVO.setTaxpayer(compInfoVO.getInvoiceVO().getTaxpayer());
            compInfoVO.setEmail(compInfoVO.getInvoiceVO().getEmail());
            JsonObject jsonObject = combatData(compInfoVO);
            if (jsonObject != null) {
                //ERP接口调用
                String url = appProperties.erpUrlPrev + "/addOrUpdateCus";
                String result = HttpRequestUtil.postJson(url, jsonObject.toString());
                LogUtil.getDefaultLogger().info("调用ERP接口结果返回： " + result);
                if (result != null && !result.isEmpty()) {
                    JsonObject object = new JsonParser().parse(result).getAsJsonObject();
                    int code = object.get("code").getAsInt();
                    if (code != 200) {//失败处理
                        int errorCode = object.get("errorcode").getAsInt();
                        optSyncErrComp(errorCode, compInfoVO.getCid());
                    } else if (code == 200) {
                        updSyncCState(compInfoVO.getCid());
                    }
                    return code;
                }
            }
        } catch (Exception e) {
            optSyncErrComp(1, compInfoVO.getCid());
            e.printStackTrace();
        }
        return -1L;
    }


    private static int updSyncCState(long syncid) {
        String updSQL = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and syncid=? and syncfrom=1";
        return baseDao.updateNative(updSQL, syncid);
    }

    @UserPermission(ignore = true)
    public boolean systemConfigIsOpen(AppContext appContext) {
        return systemConfigOpen(appContext.param.arrays[0]);
    }

    private static boolean systemConfigOpen(String varname) {
        String selectSQL = "select count(*) from {{?" + DSMConst.TB_SYSTEM_CONFIG + "}} where "
                + " cstatus&1=0 and value=1 and varname=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, varname);
        return Long.valueOf(String.valueOf(queryResult.get(0)[0])) > 0;
    }


    public static void saveCusConcat(int compId) {
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Object future = executorService.submit((Callable<Object>) () -> {
                //客户信息同步开关是否开启
                if (systemConfigOpen("CUS_SYNC")) {
                    return postSaveCusConcat(compId, 0);
                } else {
                    LogUtil.getDefaultLogger().info("客户信息同步开关未开启>>>>>>>>>>>>>>>");
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

    private static int postSaveCusConcat(int compId, int type) {
        ConsigneeVO consigneeVO = MyDrugStoreInfoModule.getDefaultConsignee(compId);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("compid", compId);
        jsonObject.addProperty("contact", consigneeVO.getContactname());
        jsonObject.addProperty("tel", consigneeVO.getContactphone());
        //ERP接口调用
        String url = appProperties.erpUrlPrev + "/saveCusConcat";
        int code = 0;
        String result;
        try {
            result = HttpRequestUtil.postJson(url, jsonObject.toString());
            LogUtil.getDefaultLogger().info("保存收货人信息调用ERP接口结果返回： " + result);
            if (result != null && !result.isEmpty()) {
                JsonObject object = new JsonParser().parse(result).getAsJsonObject();
                code = object.get("code").getAsInt();
                int errorCode = object.get("errorcode").getAsInt();
                if (code != 200 && code != -2 && type == 0) {//失败处理
                    optSyncErrComp(errorCode, compId);
//                    updConsigneeState(compId, consigneeVO.getShipid());
                } else if ((code == 200 || code == -2) && type == 1) {
                    updSyncCState(compId);
                }
            }
        } catch (Exception e) {
            if (type == 0) {
                optSyncErrComp(1, compId);
            }
            e.printStackTrace();
        }
        return code;
    }

    private static void optSyncErrComp(int errorCode, int compId) {
        SyncErrVO syncErrVO = new SyncErrVO();
        syncErrVO.setSyncfrom(1);
        syncErrVO.setSyncreason(errorCode);
        syncErrVO.setSynctype(1);
        syncErrVO.setSyncid(compId);
        syncErrVO.setSyncway(1);
        insertOrUpdSyncErr(syncErrVO);
    }


    @UserPermission(ignore = true)
    public int insertOrUpdSyncErr(AppContext appContext) {
        SyncErrVO syncErrVO = GsonUtils.jsonToJavaBean(appContext.param.json, SyncErrVO.class);
        assert syncErrVO != null;
        return baseDao.updateNative(INSERT_SYNC_SQL, GenIdUtil.getUnqId(), syncErrVO.getSynctype(), syncErrVO.getSyncid(),
                syncErrVO.getSyncmsg(), 0, syncErrVO.getSyncfrom(), syncErrVO.getSyncreason(), 1, syncErrVO.getSyncway());
    }

    @UserPermission(ignore = true)
    public int updSyncCState(AppContext appContext) {
        long syncid = Long.parseLong(appContext.param.arrays[0]);
        return updSyncCState(syncid);
    }

    private static int insertOrUpdSyncErr(SyncErrVO syncErrVO) {
        assert syncErrVO != null;
        String selectSQL = "select unqid from {{?" + DSMConst.TD_SYNC_ERROR + "}} where cstatus&1=0 "
                + " and syncid=? and syncfrom=1";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, syncErrVO.getSyncid());
        if (queryResult == null || queryResult.isEmpty()) {
            return baseDao.updateNative(INSERT_SYNC_SQL, GenIdUtil.getUnqId(), syncErrVO.getSynctype(), syncErrVO.getSyncid(),
                    syncErrVO.getSyncmsg(), 0, syncErrVO.getSyncfrom(), syncErrVO.getSyncreason(), 1, syncErrVO.getSyncway());
        }
        String unqid = String.valueOf(queryResult.get(0)[0]);
        return baseDao.updateNative(UPDATE_SYNC_SQL, syncErrVO.getSynctype(), unqid);
    }

    /**
     * @接口摘要 ERP对接返回审核结果
     * @业务场景 ERP调用
     * @传参类型 json
     * @传参列表 {compid 企业码 state 审核结果}
     * @返回列表 200 成功
     */
    @UserPermission(ignore = true)
    public Result syncCompState(AppContext appContext) {
        int code = -1;
        Result result = new Result();
        String json = appContext.param.json;
        JsonObject jsonObject = new JsonParser().parse(json).getAsJsonObject();
        int compid = jsonObject.get("compid").getAsInt();
        int state = jsonObject.get("state").getAsInt();
        String updSQL = "update {{?" + DSMConst.TB_COMP + "}} set cstatus=? where "
                + " cstatus&1=0 and cid=?";
//        if (state == 256) {
//            code = baseDao.updateNative(updSQL, 512, state, compid);
//        } else {
//            code = baseDao.updateNative(updSQL, 256, state, compid);
//        }
        return baseDao.updateNative(updSQL, state, compid) >= 0
                ? result.success("操作成功") : result.fail("操作失败");
    }


    /**
     * @接口摘要 同步企业相关信息异常处理接口
     * @业务场景 同步信息到ERP异常
     * @传参类型
     * @传参列表
     * @返回列表 200成功
     */
    @UserPermission(ignore = true)
    public Result syncErpDtOnLine(AppContext appContext) {
        Result result = new Result();
        List<Long> compIdList = new ArrayList<>();
        List<Long> orderNoList = new ArrayList<>();
        StringBuilder compIdSB = new StringBuilder();
        StringBuilder orderNoSB = new StringBuilder();
        JsonObject json = new JsonParser().parse(appContext.param.json).getAsJsonObject();
        int type = json.get("type").getAsInt();//0，自动同步 1手动干预
        String selectSQL = "select syncid,synctype,unqid from {{?" + DSMConst.TD_SYNC_ERROR + "}} where cstatus&1=0 "
                + " and syncfrom=1 ";
        if (type == 1) {
            String busArr = json.get("unqidArr").getAsString();
            selectSQL = selectSQL + " and unqid in(" + busArr + ")";
        }
        List<Object[]> cgResults = baseDao.queryNative(selectSQL);
        if (cgResults != null && cgResults.size() > 0) {
            for (Object[] cgResult : cgResults) {
                long syncid = Long.parseLong(String.valueOf(cgResult[0]));
                int synctype = Integer.parseInt(String.valueOf(cgResult[1]));
//                long unqid = Long.parseLong(String.valueOf(cgResult[2]));
                if (synctype == 1) {
                    compIdSB.append(syncid).append(",");
                    compIdList.add(syncid);
                } else if (synctype == 2) {
                    orderNoSB.append(syncid).append(",");
                    orderNoList.add(syncid);
                }
            }

        } else {
            return result.fail("无数据");
        }
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Object future = executorService.submit(() -> {
                String compIdStr, orderNoStr;
                //同步客户操作 batchAddOrUpdateCus
                if (compIdSB.toString().contains(",")) {
                    compIdStr = compIdSB.toString().substring(0, compIdSB.toString().length() - 1);
                    batchAddOrUpdateCus(compIdStr, compIdList);
                }
                //同步订单操作  batchProduceSalesOrder
                if (orderNoSB.toString().contains(",")) {
                    orderNoStr = orderNoSB.toString().substring(0, orderNoSB.toString().length() - 1);
                    batchProduceSalesOrder(orderNoStr, orderNoList);
                }
                return 0;
            }).get();
            if (future != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5 * 1000, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return result.fail("操作失败");
        }
        return result.success();

    }


    private void batchProduceSalesOrder(String orderNoStr, List<Long> syncids) {
        //ERP接口调用
        String paramsStr = IceRemoteUtil.batchProduceSalesOrder(orderNoStr, 536862720);
        String url = appProperties.erpUrlPrev + "/batchProduceSalesOrder";
        try {
            String result = HttpRequestUtil.postJson(url, paramsStr);
            LogUtil.getDefaultLogger().info("调用ERP接口结果返回： " + result);
            if (result != null && !result.isEmpty()) {
                JsonObject object = new JsonParser().parse(result).getAsJsonObject();
                String dataStr = object.get("data").toString();
                List<Object[]> params = new ArrayList<>();
                batchUpdSyncResult(dataStr, syncids, params);
                if (params.size() > 0) {
                    sendMsg2AdminPhone("订单数据同步到ERP失败，请相关人员及时处理");
                }
            }
        } catch (IOException e) {
            batchUpdSyncErr(syncids);
            sendMsg2AdminPhone("订单数据同步到ERP失败，请相关人员及时处理");
            e.printStackTrace();
        }

    }

    private void batchAddOrUpdateCus(String compIdStr, List<Long> syncids) {
        //ERP接口调用
        JsonObject jsonObject = new JsonObject();
        JsonArray list = getCompInfo(compIdStr);
        if (list == null) return;
        jsonObject.add("list", list);
        String url = appProperties.erpUrlPrev + "/batchAddOrUpdateCus";
        try {
            String result = HttpRequestUtil.postJson(url, jsonObject.toString());
            LogUtil.getDefaultLogger().info("调用ERP接口结果返回： " + result);
            if (result != null && !result.isEmpty()) {
                JsonObject object = new JsonParser().parse(result).getAsJsonObject();
                String dataStr = object.get("data").getAsString();
                List<Object[]> params = new ArrayList<>();
                batchUpdSyncResult(dataStr, syncids, params);
                if (params.size() > 0) {
                    sendMsg2AdminPhone("客户数据同步到ERP失败，请相关人员及时处理");
                }
            }
        } catch (IOException e) {
            batchUpdSyncErr(syncids);
            sendMsg2AdminPhone("客户数据同步到ERP失败，请相关人员及时处理");
            e.printStackTrace();
        }
    }

    private static void sendMsg2AdminPhone(String msg) {
        String selectSQL = "select uphone from {{?" + DSMConst.TB_SYSTEM_USER + "}} where cstatus&1=0 "
                + " and roleid&1>0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        for (Object[] qResult : queryResult) {
            SmsUtil.sendMsg(String.valueOf(qResult[0]), msg);
        }
    }

    private boolean batchUpdSyncResult(String dataStr, List<Long> syncids, List<Object[]> params) {
        String updateSyncSQL = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set syncdate=CURRENT_DATE,"
                + " synctime=CURRENT_TIME,syncreason=?,synctimes=synctimes+1 where cstatus&1=0 and "
                + " syncid=?  and syncfrom=1";
        JsonArray dataArr = new JsonParser().parse(dataStr).getAsJsonArray();
        for (JsonElement data: dataArr) {
            JsonObject obj = data.getAsJsonObject();
            int code = obj.get("code").getAsInt();
            if (code != 200 && code != 2) {//失败处理
                params.add(new Object[]{obj.get("errorcode").getAsInt(), obj.get("syncid").getAsString()});
                syncids.remove(obj.get("syncid").getAsLong());
            }
        }
        batchUpdSyncCState(syncids);
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(updateSyncSQL, params, params.size()));
    }


    private boolean batchUpdSyncErr(List<Long> unqids) {
        String updateSyncSQL = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set syncdate=CURRENT_DATE,"
                + " synctime=CURRENT_TIME,syncreason=?,synctimes=synctimes+1 where cstatus&1=0 and "
                + " unqid=?  and syncfrom=1";
        List<Object[]> params = new ArrayList<>();
        for (long unqid : unqids) {
            params.add(new Object[]{1, unqid});
        }
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(updateSyncSQL, params, params.size()));
    }

    private boolean batchUpdSyncCState(List<Long> unqids) {
        String updSQL = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set cstatus=cstatus|1 where "
                + " cstatus&1=0 and unqid=? and syncfrom=1";
        List<Object[]> params = new ArrayList<>();
        for (long unqid : unqids) {
            params.add(new Object[]{ unqid});
        }
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(updSQL, params, params.size()));
    }

    public JsonArray getCompInfo(String compidStr) {
        String sSQL = "select c.cid,cname,caddrcode,caddr,inviter,storetype,u.uphone,c.cstatus,iv.taxpayer,"
                + " iv.bankers,iv.account,iv.email, si.contactname,si.contactphone from {{?"
                + DSMConst.TB_COMP + "}} c left join {{?" + DSMConst.TB_SYSTEM_USER + "}} u "
                + " on c.cid = u.cid left join {{?" + DSMConst.TB_COMP_INVOICE + "}} iv on "
                + " c.cid=iv.cid left join {{?" + DSMConst.TB_COMP_SHIP_INFO + "}} si on " +
                " c.cid=si.compid where c.cstatus&1=0 and u.cstatus&1=0  and c.cid in(" + compidStr + ")"
                + " group by c.cid";
        List<Object[]> queryResult = baseDao.queryNative(sSQL);
        if (queryResult == null || queryResult.isEmpty()) return null;
        CompInfoVO[] compInfoVOS = new CompInfoVO[queryResult.size()];
        baseDao.convToEntity(queryResult, compInfoVOS, CompInfoVO.class);
        List<CompInfoVO> compInfoVOList = Arrays.asList(compInfoVOS);
        return setBatchApt(compidStr, compInfoVOList);
    }

    private JsonArray setBatchApt(String compIdStr, List<CompInfoVO> compInfoVOS) {
        JsonArray list = new JsonArray();
        HashMap<Integer, List<AptitudeVO>> aptMap = new HashMap<>();
        String selectSQL = "select aptid,compid,atype,certificateno,validitys,validitye,cstatus,pname "
                + " from {{?" + DSMConst.TB_COMP_APTITUDE + "}} where cstatus&1=0 and compid in(" + compIdStr + ")";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return list;
        AptitudeVO[] aptitudeVOS = new AptitudeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, aptitudeVOS, AptitudeVO.class);
        for (AptitudeVO aptitudeVO : aptitudeVOS) {
            if (aptMap.containsKey(aptitudeVO.getCompid())) {
                aptMap.get(aptitudeVO.getCompid()).add(aptitudeVO);
            } else {
                aptMap.put(aptitudeVO.getCompid(), Collections.singletonList(aptitudeVO));
            }
        }
        for (CompInfoVO compInfoVO : compInfoVOS) {
            if (aptMap.containsKey(compInfoVO.getCid())) {
                compInfoVO.setAptitudeVOS(aptMap.get(compInfoVO.getCid()));
            }
            list.add(combatData(compInfoVO));
        }
        return list;
    }

    @UserPermission(ignore = true)
    public Result erpBatch2SyncErr(AppContext appContext) {
        JsonArray syncErrArr = new JsonParser().parse(appContext.param.json).getAsJsonArray();
        List<Object[]> params = new ArrayList<>();
        for (JsonElement syncErr : syncErrArr) {
            SyncErrVO syncErrVO = GsonUtils.jsonToJavaBean(syncErr.getAsJsonObject().toString(), SyncErrVO.class);
            assert syncErrVO != null;
            params.add(new Object[]{syncErrVO.getUnqid(), syncErrVO.getSynctype(), syncErrVO.getSyncid(),
                    syncErrVO.getSyncmsg(), 0, syncErrVO.getSyncdate(),syncErrVO.getSynctime(),
                    syncErrVO.getSyncfrom(), syncErrVO.getSyncreason(),
                    1, 2});
        }
        String sql =  "insert into {{?" + DSMConst.TD_SYNC_ERROR + "}} "
                + " (unqid,synctype,syncid,syncmsg,cstatus,"
                + "syncdate,synctime,syncfrom,syncreason,synctimes,"
                + "syncway)  values(?,?,?,?,?,?,?,?,?,?,?)";
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(sql, params, params.size()))
                ? new Result().success() : new Result().fail("操作失败");
    }

    @UserPermission(ignore = true)
    public Result erpBatchUpd2SyncErr(AppContext appContext) {
        JsonArray syncErrArr = new JsonParser().parse(appContext.param.json).getAsJsonArray();
        List<Object[]> params = new ArrayList<>();
        for (JsonElement syncErr : syncErrArr) {
            SyncErrVO syncErrVO = GsonUtils.jsonToJavaBean(syncErr.getAsJsonObject().toString(), SyncErrVO.class);
            assert syncErrVO != null;
            params.add(new Object[]{syncErrVO.getSyncdate(),syncErrVO.getSynctime(),
                    syncErrVO.getSyncreason(), syncErrVO.getUnqid()});
        }
        String sql = "update {{?" + DSMConst.TD_SYNC_ERROR + "}} set syncdate=?,"
                + " synctime=?,syncreason=?,synctimes=synctimes+1 where cstatus&1=0 and "
                + " unqid=? ";
        return !ModelUtil.updateTransEmpty(baseDao.updateBatchNative(sql, params, params.size()))
                ? new Result().success() : new Result().fail("操作失败");
    }

}
