package com.onek.user;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.ConsigneeVO;
import com.onek.user.service.MyConsigneeImpl;
import constant.DSMConst;
import dao.BaseDAO;
import redis.IRedisCache;
import redis.proxy.CacheProxyInstance;
import redis.util.RedisUtil;
import util.ModelUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 我的药店信息接口模块
 * @time 2019/3/21 11:13
 **/
public class MyDrugStoreInfoModule {
    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();
    private static IRedisCache myCgProxy = CacheProxyInstance.createInstance(new MyConsigneeImpl());
    
    /* *
     * @description 查询
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/3/21 13:51
     * @version 1.1.1
     **/
    public Result queryMyConsignee(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compId = jsonObject.get("compid").getAsInt();
        String selectSQL = "select shipid,compid,contactname,contactphone,cstatus from {{?"
                + DSMConst.D_COMP_SHIP_INFO + "}} where cstatus&1=0 and compid=" + compId;
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
//        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        ConsigneeVO[] consigneeVOS = new ConsigneeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, consigneeVOS, ConsigneeVO.class);
        return result.success(consigneeVOS);
    }

    /* *
     * @description 新增修改
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/3/21 13:51
     * @version 1.1.1
     **/
    public Result insertOrUpdConsignee(AppContext appContext) {
        Result result = new Result();
        int code;
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compId = jsonObject.get("compid").getAsInt();
        String contactName = jsonObject.get("contactname").getAsString();
        long contactPhone = jsonObject.get("contactphone").getAsLong();
        int shipId = jsonObject.get("shipid").getAsInt();
        if (queryCount(compId, shipId, contactPhone,2) >= 5) {
            return result.fail("收货人已达到上限！");
        }
        if (queryCount(compId, shipId, contactPhone,1) > 0) {
            return result.fail("收货人已存在！");
        }
        String insertSQL = "insert into {{?" + DSMConst.D_COMP_SHIP_INFO + "}} "
                + "(shipid, compid, contactname, contactphone)"
                + " values(?,?,?,?)";
        String updSQL = "update {{?" + DSMConst.D_COMP_SHIP_INFO + "}} set contactname=?, contactphone=? "
                + " where cstatus&1=0 and shipid=?";
        if (shipId > 0) {
            code = baseDao.updateNative(updSQL, contactName, contactPhone, shipId);
        } else {
            shipId = Math.toIntExact(RedisUtil.getStringProvide().increase("SHIP_ID"));
            code = baseDao.updateNative(insertSQL, shipId, compId, contactName, contactPhone);
        }
        myCgProxy.update(null);
        return code > 0 ? result.success("操作成功") : result.fail("操作失败");
    }

    private int queryCount(int compid, int shipId, long contactPhone, int type) {
        String selectSQL;
        List<Object[]> queryResult;
        switch (type) {
            case 1:
                selectSQL = "select count(*) from {{?" + DSMConst.D_COMP_SHIP_INFO
                        + "}} where cstatus&1=0 and compid=" + compid + " and contactphone=" + contactPhone;
                queryResult = baseDao.queryNative(selectSQL);
                return Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
            case 2:
                selectSQL = "select count(*) from {{?" + DSMConst.D_COMP_SHIP_INFO
                        + "}} where cstatus&1=0 and compid=" + compid
                        + " and contactphone=" + contactPhone + " and shipid<>" + shipId;
                queryResult = baseDao.queryNative(selectSQL);
                return Integer.parseInt(String.valueOf(queryResult.get(0)[0]));
            default:
                return 0;
        }
    }

    /* *
     * @description 设置默认
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/3/21 13:51
     * @version 1.1.1
     **/
    public Result setDefault(AppContext appContext) {
        Result result = new Result();
        List<Object[]> params = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compId = jsonObject.get("compid").getAsInt();
        int shipId = jsonObject.get("shipid").getAsInt();
        String updateSQL = "update {{?" + DSMConst.D_COMP_SHIP_INFO + "}} set cstatus=cstatus&~2 "
                + " where cstatus&1=0 and compid=? and cstatus&2>0";
        params.add(new Object[]{compId});
        String updateSQLT = "update {{?" + DSMConst.D_COMP_SHIP_INFO + "}} set cstatus=cstatus|2 "
                + " where cstatus&1=0 and shipid=?";
        params.add(new Object[]{shipId});
        int[] arr = baseDao.updateTransNative(new String[]{updateSQL, updateSQLT}, params);
        boolean b = !ModelUtil.updateTransEmpty(arr);
        myCgProxy.update(null);
        return b ? result.success("操作成功") : result.fail("操作失败");
    }

    /* *
     * @description 删除
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/3/21 13:52
     * @version 1.1.1
     **/
    public Result delConsignee(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int shipId = jsonObject.get("shipid").getAsInt();
        String delSQL = "update {{?" + DSMConst.D_COMP_SHIP_INFO + "}} set cstatus=cstatus|1 "
                + " where cstatus&1=0 and shipid=" + shipId;
        myCgProxy.del(null);
        return baseDao.updateNative(delSQL) > 0 ? result.success("操作成功") : result.fail("操作失败");
    }

    public Result queryDefaultCg(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int compId = jsonObject.get("compid").getAsInt();
        ConsigneeVO consigneeVO = (ConsigneeVO) myCgProxy.getId(compId);
        return result.success(consigneeVO);
    }



}
