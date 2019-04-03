package com.onek.discount;

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
 * @author Administrator
 * @version V1.0
 * @ClassName CommonModule
 * @Description TODO
 * @date 2019-04-02 12:01
 */
public class CommonModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();
    /**
     * @description 查询所有活动优惠券规则
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/4/1 11:55
     * @version 1.1.1
     **/

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
    public Result queryRules(AppContext appContext) {
        Result result = new Result();
        String selectSQL = "select unqid,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} where cstatus&1=0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"unqid", "rulename"});
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


}
