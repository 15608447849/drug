package com.onek.discount;

import Ice.Current;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.discount.entity.PromGiftVO;
import com.onek.discount.entity.RulesVO;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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
     * @description 查询赠品信息
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryPromGift(AppContext appContext) {
        Result result = new Result();
        String selectSQL = "select unqid giftno,giftname from {{?" + DSMConst.TD_PROM_GIFT + "}} where cstatus&1=0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        PromGiftVO[] promGiftVOS = new PromGiftVO[queryResult.size()];
        baseDao.convToEntity(queryResult, promGiftVOS, PromGiftVO.class, new String[]{"giftno", "giftname"});
        return result.success(promGiftVOS);
    }

    /**
     * @description 查询活动规则
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/
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
     * @description 查询优惠券规则
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/
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
                + " and brulecode REGEXP '^2' and  NOT EXISTS(select brulecode from {{?"
                + DSMConst.TD_PROM_COUPON +"}} b where cstatus & ? >0 and cstatus&1=0 and a.brulecode = b.brulecode and "
                 + " brulecode REGEXP '^2' and enddate>CURRENT_DATE)";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,coupType);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"brulecode", "rulename"});
        return result.success(rulesVOS);
    }


    /**
     * @description 查询活动优惠券规则
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/
    @UserPermission(ignore = true)
    public Result queryCoupAssRules(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
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
     * @description 查询规则
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/
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
     * @description 参加资格
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/2 21:46
     * @version 1.1.1
     **/
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
