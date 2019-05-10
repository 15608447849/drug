package com.onek.global.message;

import dao.BaseDAO;

import java.util.List;
import java.util.Locale;

import static constant.DSMConst.D_SMS_TEMPLATE;

/**
 * lzp
 * 消息模板处理工具
 **/
public class MessageTemplateUtil {
    //模板编号+参数 -> 内容
    public static String templateConvertMessage(int tempNo,Object... args){
        String msg = getTmpByTno(tempNo);
        if (msg == null) return "";
        if (args!=null && args.length>0) msg = String.format(Locale.CHINA,msg,args);
        return msg;
    }
    //数据库获取模板
    private static String getTmpByTno(int tno) {
        String selectSql = "SELECT tcontext FROM {{?" + D_SMS_TEMPLATE + "}} WHERE cstatus&1=0 and tno= ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,tno);
        if (lines.size() == 1){
            return lines.get(0)[0].toString();
        }
        return null;
    }

    public static String messageTempStatus(int tno) {
        String selectSql = "SELECT cstatus FROM {{?" +D_SMS_TEMPLATE+ "}} WHERE tno = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,tno);
        if (lines.size() == 1){
            return lines.get(0)[0].toString();
        }
        return "0";
    }
}
