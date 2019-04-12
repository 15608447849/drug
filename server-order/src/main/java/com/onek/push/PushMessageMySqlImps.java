package com.onek.push;

import com.onek.server.infimp.IPushMessageStore;
import dao.BaseDAO;
import global.IceRemoteUtil;
import util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Ice.Application.communicator;
import static constant.DSMConst.TD_PUSH_MSG;
import static global.GenIdUtil.getUnqId;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/10 11:28
 * 推送服务 数据存储
 */
public class PushMessageMySqlImps implements IPushMessageStore {
    @Override
    public long storeMessageToDb(String identityName, String message) {
        try {
            int compid = Integer.parseInt(identityName);
            long unqid = getUnqId();

            String insertSql = "INSERT INTO {{?"+TD_PUSH_MSG+"}} ( unqid, identity, message, date, time ) " +
                    "VALUES " +
                    "( ?, ?, ? , CURRENT_DATE,CURRENT_TIME )";
            int i = BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                    insertSql,
                    unqid, compid, message);
            if (i > 0){
                communicator().getLogger().print("推送-存储消息\t"+identityName+" , "+ message);
                return unqid;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void changeMessageStateToDb(String identityName,long id) {

        try {
            int compid = Integer.parseInt(identityName);

            String updateSql = "UPDATE {{?"+TD_PUSH_MSG+"}} SET cstatus=1 WHERE unqid = ? AND identity = ?";

            int i = BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                    updateSql,
                    id,compid );

            if (i > 0){
                communicator().getLogger().print("推送成功-改变数据\t"+id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<Long,String> checkOfflineMessageFromDbByIdentityName(String identityName) {

        Map<Long,String> map = new HashMap<>();
        try {
            int compid = Integer.parseInt(identityName);

            String selectSql = "SELECT unqid,message " +
                    "FROM {{?"+TD_PUSH_MSG+"}} " +
                    "WHERE cstatus=0 AND identity = ? " +
                    "ORDER BY date,time"; //DESC
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compid,getCurrentYear(),
                    selectSql,compid);
            for (Object[] arr: lines){
                map.put((long)arr[0],arr[1].toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        communicator().getLogger().print("查询-离线数据\t"+identityName+" - 条数:"+ map.size());
        return map;
    }

    @Override
    public String convertMessage(String identityName, String message) {
        //规则格式:  push:模板序列#参数内容
        try {
            if (message.startsWith("push")) {
                String arrayStr = message.split(":")[1];
                String[] array ;
                if (arrayStr.contains("#")){
                    array = arrayStr.split("#");
                }else{
                    array = new String[]{arrayStr};
                }
                String msg = IceRemoteUtil.getMessageByNo(array);
                if (!StringUtils.isEmpty(msg)) return msg;
            }
        } catch (Exception ignored) {

        }

        return message;
    }


}
