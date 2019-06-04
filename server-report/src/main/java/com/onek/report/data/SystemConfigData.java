package com.onek.report.data;

import constant.DSMConst;
import dao.BaseDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dao.BaseDAO.getBaseDAO;

public class SystemConfigData {

    public static Map<String,String> VALS_MAP = new HashMap<>();

    private static String CONFIG_SQL = "select varname,value from {{?"+ DSMConst.TB_SYSTEM_CONFIG +"}} where cstatus&1 = 0";

    public static void init(){
        List<Object[]> list = BaseDAO.getBaseDAO().queryNative(CONFIG_SQL);
        if(list != null && list.size() > 0){
            for(Object[] arr : list){
                VALS_MAP.put(arr[0].toString(), arr[1].toString());
            }
        }
    }

    public static String getValByVarName(String varName){
        return VALS_MAP.get(varName);
    }
}
