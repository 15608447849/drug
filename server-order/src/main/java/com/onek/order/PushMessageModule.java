package com.onek.order;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
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

    private static class MessageBean{
        String msg;
        String time;

        MessageBean(String msg, String date, String time) {
            this.msg = msg;
            this.time = date+" "+time;
        }
    }
    /**
     * 前端全部消息列表 查询
     * @param appContext
     * @return
     */
    public Result queryMessage(AppContext appContext){
        Result result = new Result();
        List<MessageBean> list = new ArrayList<>();
        Page page = new Page();
        PageHolder pageHolder = new PageHolder(page);
        page.pageIndex = appContext.param.pageIndex;
        page.pageSize  = appContext.param.pageNumber;
        try {
            int compid = appContext.getUserSession().compId;
            String selectSql = "SELECT message,date,time " +
                    "FROM {{?"+TD_PUSH_MSG+"}} " +
                    "WHERE cstatus=1 AND identity = ? " +
                    "ORDER BY date DESC,time DESC";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compid,getCurrentYear(),
                    pageHolder, page,selectSql,compid);

            for (Object[] arr: lines){
                String msg = StringUtils.obj2Str(arr[0]);
                if (StringUtils.isEmpty(msg) || !msg.startsWith("push")) continue;
                String date = StringUtils.obj2Str(arr[1]);
                String time = StringUtils.obj2Str(arr[2]);
                list.add(new MessageBean(convertPushMessage("",msg),date,time));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        result.setQuery(list,pageHolder);
        return result;
    }


}
