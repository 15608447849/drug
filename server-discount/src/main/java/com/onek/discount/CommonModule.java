package com.onek.discount;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.discount.entity.PromGiftVO;
import com.onek.discount.entity.RulesVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @服务名 discountServer
 * @author Administrator
 * @version V1.0
 * @ClassName CommonModule
 * @Description TODO
 * @date 2019-04-02 12:01
 */
public class CommonModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String SELECT_LADDER_NO = "select IFNULL(max(right(offercode,2)),0) from {{?" + DSMConst.TD_PROM_LADOFF + "}} ";


    /**
     * @接口摘要 查询赠品信息
     * @业务场景 下拉查询赠品
     * @传参类型
     * @传参列表
     * @返回列表 PromGiftVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryPromGift(AppContext appContext) {
        Result result = new Result();
        String selectSQL = "select unqid giftno,giftname, giftdesc from {{?" + DSMConst.TD_PROM_GIFT + "}} where cstatus&1=0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        PromGiftVO[] promGiftVOS = new PromGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, promGiftVOS, PromGiftVO.class, new String[]{"giftno", "giftname", "desc"});
        return result.success(promGiftVOS);
    }

    /**
     * @接口摘要 保存赠品
     * @业务场景 ERP专用
     * @传参类型 json
     * @传参列表 com.onek.discount.entity.PromGiftVO
     * @返回列表 200 成功 -1 失败
     */
    @UserPermission(ignore = true)
    public Result saveGift(AppContext appContext) {
        PromGiftVO gift = JSON.parseObject(appContext.param.json, PromGiftVO.class);

        if (gift == null) {
            return new Result().fail("参数为空");
        }

        String sql =
                " SELECT unqid, giftname, giftdesc "
                + " FROM {{?" + DSMConst.TD_PROM_GIFT + "}} "
                + " WHERE cstatus&1=0 AND unqid = ? ";

        List<Object[]> queryResult = baseDao.queryNative(sql, gift.getGiftno());

        if (queryResult.isEmpty()) {
            sql = " INSERT INTO {{?" + DSMConst.TD_PROM_GIFT + "}} "
                    + " (unqid, giftname, giftdesc, cstatus) "
                    + " VALUES(?, ?, ?, ?) ";

            baseDao.updateNative(sql,
                    gift.getGiftno(), gift.getGiftname(),
                    gift.getDesc(), 0);
        } else {
            sql = " UPDATE {{?" + DSMConst.TD_PROM_GIFT + "}} "
                    + " SET giftname = ?, giftdesc = ? "
                    + " WHERE cstatus&1 = 0 AND unqid = ? ";

            baseDao.updateNative(sql,
                    gift.getGiftname(), gift.getDesc(), gift.getGiftno());
        }




        return new Result().success();
    }


    /**
     * @接口摘要 查询活动规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {type 活动规则码}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryRules(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int brulecode = jsonObject.get("type").getAsInt();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode like '" + brulecode + "%'";
//        String selectSQLEx = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
//                + " and brulecode like '" + brulecode + "%' and  NOT EXISTS(select brulecode from {{?"
//                + DSMConst.TD_PROM_ACT +"}} b where cstatus&1=0 and a.brulecode = b.brulecode and brulecode like '"
//                + brulecode +"%' and edate>CURRENT_DATE)";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询优惠券规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {couptype 优惠券规则码}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryCoupRules(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
//        int brulecode = jsonObject.get("type").getAsInt();
        int coupType = jsonObject.get("couptype").getAsInt();
//        String selectActSQL = " SELECT brulecode FROM {{?" + DSMConst.TD_PROM_ACT +"}} where cstatus&1=0 and "
//                + " brulecode like '" + brulecode + "%' and edate>CURRENT_DATE" ;
//        List<Object[]> queryActResult = baseDao.queryNative(selectActSQL);

        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' ";

        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询活动优惠券规则
     * @业务场景 下拉查询
     * @传参类型
     * @传参列表
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryCoupAssRules(AppContext appContext) {
        Result result = new Result();
//        String json = appContext.param.json;
//        JsonParser jsonParser = new JsonParser();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} a where cstatus&1=0 "
                + " and brulecode REGEXP '^2' and  NOT EXISTS(select brulecode from {{?"
                + DSMConst.TD_PROM_COUPON +"}} b where cstatus & 128 >0 and cstatus&1=0 and a.brulecode = b.brulecode and "
                + " brulecode REGEXP '^2')";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @接口摘要 查询规则
     * @业务场景 下拉查询
     * @传参类型 json
     * @传参列表 {type 满减还是满赠}
     * @返回列表 RulesVO对象数组
     */
    @UserPermission(ignore = true)
    public Result queryRulesByType(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        int type = jsonObject.get("type").getAsInt();
        String selectSQL = "select brulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}}  where cstatus&1=0 "
                + " and brulecode REGEXP '^" + type + "'";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }

    
    /**
     * @接口摘要 参加资格 已弃用
     * @业务场景 下拉查询
     * @传参类型
     * @传参列表
     * @返回列表
     */
    public Result queryQual(AppContext appContext) {
        Result result = new Result();
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "所有会员");
        map.put(1, "采购订单数");
        map.put(2, "会员等级");
        map.put(3, "特定区域会员会员");
        return result.success(map);
    }


    public static int[] getLaderNo(String preLader,int size){
        if(size <= 0){
            return null;
        }
        StringBuilder sb = new StringBuilder(SELECT_LADDER_NO);
        sb.append(" where offercode like '");
        sb.append(preLader);
        sb.append("%' and cstatus & 1 = 0 ");
        List<Object[]> queryResult = baseDao.queryNative(sb.toString());
        int ladernum = Integer.parseInt(queryResult.get(0)[0].toString());
        int [] laddnumArray = new int[size];
        for(int i = 0; i < laddnumArray.length; i++){
            ladernum = ladernum +1;
            sb.setLength(0);
            sb.append(preLader);
            if(ladernum < 10){
                sb.append("0");
            }
            sb.append(ladernum);
            laddnumArray[i] = Integer.parseInt(sb.toString());
        }
        return laddnumArray;
    }

}
