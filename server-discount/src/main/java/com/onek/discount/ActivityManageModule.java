package com.onek.discount;

import com.onek.context.AppContext;
import com.onek.discount.entity.RulesVO;
import com.onek.entitys.Result;
import constant.DSMConst;
import dao.BaseDAO;

import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 活动管理
 * @time 2019/4/1 11:19
 **/
public class ActivityManageModule {

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
    public Result queryRules(AppContext appContext) {
        Result result = new Result();
        String selectSQL = "select unqid,rulename from {{?" + DSMConst.TD_PROM_RULE + "}} where cstatus&1=0 ";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL);
        RulesVO[] rulesVOS = new RulesVO[queryResult.size()];
        baseDao.convToEntity(queryResult, rulesVOS, RulesVO.class, new String[]{"unqid", "rulename"});
        return result.success(rulesVOS);
    }

}
