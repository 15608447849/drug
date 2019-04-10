package com.onek;

import com.onek.server.infimp.IPushMessageStore;
import dao.BaseDAO;

import java.util.ArrayList;
import java.util.List;

import static Ice.Application.communicator;
import static constant.DSMConst.TD_PUSH_MSG;
import static constant.DSMConst.TD_TRAN_COLLE;
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
    public List<String> checkOfflineMessageFromDbByIdentityName(String identityName) {

        ArrayList<String> list = new ArrayList<>();
        try {
            int compid = Integer.parseInt(identityName);

            String selectSql = "SELECT message " +
                    "FROM {{?"+TD_PUSH_MSG+"}} " +
                    "WHERE cstatus=0 AND identity = ? " +
                    "ORDER BY date,time"; //DESC
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compid,getCurrentYear(),
                    selectSql,compid);
            for (Object[] arr: lines){
                list.add(arr[0].toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        communicator().getLogger().print("查询-离线数据\t"+identityName+" - 条数:"+ list.size());
        return list;
    }
}
