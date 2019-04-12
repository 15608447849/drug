package com.onek.global.message;

import constant.DSMConst;
import dao.BaseDAO;

import java.util.List;
import java.util.Locale;

/**
 * @author 11842
 * @version 1.1.1
 * @description
 * @time 2019/3/19 16:28
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
        String selectSql = "SELECT tcontext FROM {{?" + DSMConst.D_SMS_TEMPLATE + "}} WHERE cstatus&1=0 and tno=" +tno;
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
        if (lines.size() == 1){
            return lines.get(0)[0].toString();
        }
        return null;
    }
}
