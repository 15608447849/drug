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
     * @description 查询所有活动优惠券规则
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
        int type = jsonObject.get("type").getAsInt();
        int cstatus = 64;
        if(type == 2){
            cstatus = 128;
        }

        String selectSQL = "select rulecode,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} where cstatus & ? > 0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL,cstatus);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"rulecode", "rulename"});
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

    public static void main(String[] args) {
        CommonModule commonModule = new CommonModule();
        IRequest request = new IRequest();
        Current current = new Current();
        request.param.json = "";
        AppContext appContext = new AppContext(current,request);

        Result result = commonModule.queryPromGift(appContext);
        System.out.println(result.toString());
    }


}
