package com.onek.user;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.user.entity.CompInfoVO;
import com.onek.user.entity.ConsigneeVO;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.http.HttpRequestUtil;

import java.util.List;
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

    private static JsonObject combatData(CompInfoVO compInfoVO) {
        int compId = compInfoVO.getCid();
        if (compId > 0) {
            ConsigneeVO consigneeVO = MyDrugStoreInfoModule.getDefaultConsignee(compId);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("compid", compId);
            jsonObject.addProperty("cname", compInfoVO.getCname());
            jsonObject.addProperty("caddrcode", compInfoVO.getCaddrcode());
            jsonObject.addProperty("taxpayer", compInfoVO.getInvoiceVO().getTaxpayer());
            jsonObject.addProperty("bankers", compInfoVO.getInvoiceVO().getBankers());
            jsonObject.addProperty("account", compInfoVO.getInvoiceVO().getAccount());
            jsonObject.addProperty("corporation", "");
            jsonObject.addProperty("caddr", compInfoVO.getCaddr());
            jsonObject.addProperty("storetype", compInfoVO.getStoretype());
            jsonObject.addProperty("staddr", "");
            jsonObject.addProperty("contact", consigneeVO.getContactname());
            jsonObject.addProperty("tel", consigneeVO.getContactphone());
            jsonObject.addProperty("principal", "");
            jsonObject.addProperty("qcprincipal", "");
            jsonObject.addProperty("email", compInfoVO.getInvoiceVO().getEmail());
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
                    return postAddOrUpdateCus(compInfoVO, 0);
                } else {
                    LogUtil.getDefaultLogger().info("客户信息同步开关未开启>>>>>>>>>>>>>>>");
                }
                return -1;
            }).get();
            if (future != null) {
                executorService.shutdown();
                if (!executorService.awaitTermination(5*1000, TimeUnit.MILLISECONDS)){
                    executorService.shutdownNow();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

    }

    private static long postAddOrUpdateCus(CompInfoVO compInfoVO, int type){
        try {
            JsonObject jsonObject = combatData(compInfoVO);
            if (jsonObject != null) {
                //ERP接口调用
                String url = appProperties.erpUrlPrev + "/addOrUpdateCus";
//                System.out.println("postJson--->> " + jsonObject.toString());
                String result = HttpRequestUtil.postJson(url, jsonObject.toString());
                LogUtil.getDefaultLogger().info("调用ERP接口结果返回： " + result);
                if (result != null && !result.isEmpty()) {
                    int code = new JsonParser().parse(result).getAsJsonObject().get("code").getAsInt();
                    if (code != 200 && type == 0) {//失败处理
                        updateCompState(compInfoVO.getCid());
                    } else if (code == 200 && type == 1) {
                        updateCState(compInfoVO.getCid());
                    }
                    return code;
                }
            }
        } catch (Exception e) {
            if (type == 0) {
                updateCompState(compInfoVO.getCid());
            }
            e.printStackTrace();
        }
        return -1L;
    }

    /* *
     * @description 同步失败处理
     * @params [compid]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/24 14:38
     * @version 1.1.1
     **/
    private static void updateCompState(int compid) {
        String updSQL = "update {{?" + DSMConst.TB_COMP + "}} set cstatus=cstatus|4096 where "
                + " cstatus&1=0 and cid=?";
        baseDao.updateNative(updSQL, compid);
    }

    private static void updateCState(int compid) {
        String updSQL = "update {{?" + DSMConst.TB_COMP + "}} set cstatus=cstatus&~4096 where "
                + " cstatus&1=0 and cid=?";
        baseDao.updateNative(updSQL, compid);
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
                if (!executorService.awaitTermination(5*1000, TimeUnit.MILLISECONDS)){
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
                code = new JsonParser().parse(result).getAsJsonObject().get("code").getAsInt();
                if (code != 200 && code != -2 && type == 0) {//失败处理
                    updConsigneeState(compId, consigneeVO.getShipid());
                } else if ((code == 200 || code == -2) && type == 1) {
                    updConsState(compId, consigneeVO.getShipid());
                }
            }
        } catch (Exception e) {
            if (type == 0) {
                updConsigneeState(compId, consigneeVO.getShipid());
            }
            e.printStackTrace();
        }
        return code;
    }

    /* *
     * @description 收货人信息同步到ERP失败记录
     * @params [compid, shipId]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/6/24 15:51
     * @version 1.1.1
     **/
    private static void updConsigneeState(int compid, int shipId) {
        String updSQL = "update {{?" + DSMConst.TB_COMP_SHIP_INFO + "}} set cstatus=cstatus|4096 where "
                + " cstatus&1=0 and compid=? and shipid=?";
        baseDao.updateNative(updSQL, compid, shipId);
    }

    private static void updConsState(int compid, int shipId) {
        String updSQL = "update {{?" + DSMConst.TB_COMP_SHIP_INFO + "}} set cstatus=cstatus&~4096 where "
                + " cstatus&1=0 and compid=? and shipid=?";
        baseDao.updateNative(updSQL, compid, shipId);
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
        String updSQL = "update {{?" + DSMConst.TB_COMP + "}} set cstatus=cstatus&~?|? where "
                + " cstatus&1=0 and cid=?";
        if (state == 256) {
            code = baseDao.updateNative(updSQL, 512, state, compid);
        } else {
            code = baseDao.updateNative(updSQL, 256, state, compid);
        }
        return code >= 0 ? result.success("操作成功") : result.fail("操作失败");
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
        String selectCompSQL = "select cid from {{?" + DSMConst.TB_COMP + "}} where cstatus&1=0 and "
                + " cstatus&4096>0";
        List<Object[]> queryResult = baseDao.queryNative(selectCompSQL);
        String selectCgSQL = "select compid from {{?" + DSMConst.TB_COMP_SHIP_INFO + "}} where cstatus&1=0 and "
                + " cstatus&4096>0";
        List<Object[]> cgResults = baseDao.queryNative(selectCgSQL);
        try {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Object future = executorService.submit(() -> {
                if (queryResult != null && queryResult.size() > 0) {
                    for (Object[] objects:queryResult) {
                        CompInfoVO compInfoVO = new BackGroundProxyMoudule().getCompInfo((int)objects[0]);
                        postAddOrUpdateCus(compInfoVO,1);
                    }
                }
                if (cgResults != null && cgResults.size() > 0) {
                    for (Object[] cgResult:cgResults) {
                        postSaveCusConcat((int)cgResult[0], 1);
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
