package com.onek.order;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.onek.push.PushMessageMySqlImps.convertPushMessage;
import static constant.DSMConst.TD_PUSH_MSG;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/26 16:18
 */
public class PushMessageModule {

    /**
     * 前端全部消息列表 查询
     * @param appContext
     * @return
     */
    public Result queryMessage(AppContext appContext){
        Result result = new Result();
        try {
            int compid = appContext.getUserSession().compId;
            String selectSql = "SELECT message " +
                    "FROM {{?"+TD_PUSH_MSG+"}} " +
                    "WHERE cstatus=1 AND identity = ? " +
                    "ORDER BY date,time";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compid,getCurrentYear(),
                    selectSql,compid);

            List<String> list = new ArrayList<>();
            for (Object[] arr: lines){
                String msg = StringUtils.obj2Str(arr[0]);
                if (StringUtils.isEmpty(msg) || !msg.startsWith("push")) continue;
                list.add(convertPushMessage("",msg));
            }
        result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


}
