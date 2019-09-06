package com.onek.push;

import com.onek.server.infimp.IPushMessageStore;
import com.onek.util.IceRemoteUtil;
import dao.BaseDAO;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static Ice.Application.communicator;
import static com.onek.util.GenIdUtil.getUnqId;
import static constant.DSMConst.TD_PUSH_MSG;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/10 11:28
 * 推送服务 数据存储
 */
public class PushMessageMySqlImps implements IPushMessageStore {



    @Override
    public long storeMessageToDb(IPMessage message) {
        try {
            if (!message.content.startsWith("push:")) return 0;//不存储 系统外的信息

            int compid = Integer.parseInt(message.identityName);
            long unqid = getUnqId();

            String insertSql = "INSERT INTO {{?"+TD_PUSH_MSG+"}} ( unqid, identity, message, date, time ) " +
                    "VALUES " +
                    "( ?, ?, ? , CURRENT_DATE,CURRENT_TIME )";
            int i = BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                    insertSql,
                    unqid, compid, message.content);
            if (i > 0){
                communicator().getLogger().print("推送-存储消息\t"+message.identityName+" , "+ message.content);
                return unqid;
            }
        } catch (Exception e) {
            communicator().getLogger().error("推送-存储消息失败\t"+message.identityName+" , "+ message.content +", 原因:"+ e);
        }
        return 0;
    }

    @Override
    public void changeMessageStateToDb(IPMessage message) {

        try {
            if (message.id == 0) return;
            int compid = Integer.parseInt(message.identityName);
            int status = 1;
            if (!message.content.startsWith("push")){
                status++;
            }
            String updateSql = "UPDATE {{?"+TD_PUSH_MSG+"}} SET cstatus=? WHERE unqid = ? AND cstatus=0";
            BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                    updateSql,status, message.id);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<IPMessage> checkOfflineMessageFromDbByIdentityName(String identityName) {

        List<IPMessage> list = new ArrayList<>();
        try {
            int compid = Integer.parseInt(identityName);

            String selectSql = "SELECT unqid,message " +
                    "FROM {{?"+TD_PUSH_MSG+"}} " +
                    "WHERE cstatus=0 AND identity = ? " +
                    "ORDER BY date,time"; //DESC
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compid,getCurrentYear(),
                    selectSql,compid);
            for (Object[] arr: lines){
                list.add(new IPMessage(
                        identityName,
                        (long)arr[0],
                        arr[1].toString()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list.size() > 0){
            communicator().getLogger().print("推送-离线数据\t"+identityName+" - 条数:"+ list.size());
        }
        return list;
    }



    public static String convertPushMessage(String prev,String message){

        if (message.startsWith("push")) {
            String arrayStr = message.replace("push:","");
            String[] array ;
            if (arrayStr.contains("#")){
                array = arrayStr.split("#");
            }else{
                array = new String[]{arrayStr};
            }
            String msg = IceRemoteUtil.getMessageByNo(array);
            if (!StringUtils.isEmpty(msg)) return prev+msg;
        }
        return message;
    }

    @Override
    public String convertMessage(IPMessage message) {
        //规则格式:  push:模板序列#参数内容
        return convertPushMessage("sys:" ,message.content);
    }




}
