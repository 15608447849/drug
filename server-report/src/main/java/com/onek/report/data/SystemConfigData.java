package com.onek.report.data;

import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemConfigData {

    public static final String DAY_ACTIVE_NUM	= "DAY_ACTIVE_NUM"; //市场分析报表-日活跃数量基准数
    public static final String WEEK_ACTIVE_NUM = "WEEK_ACTIVE_NUM"; //	市场分析报表-周活跃数量基准数
    public static final String MONTH_ACTIVE_NUM = "MONTH_ACTIVE_NUM"; //	市场分析报表-月活跃数量基准数
    public static final String YEAR_ACTIVE_NUM = "YEAR_ACTIVE_NUM"; //	市场分析报表-年活跃数量基准数
    public static final String DAY_REPURCHASE_NUM	= "DAY_REPURCHASE_NUM"; //	市场分析报表-日复购数量基准数
    public static final String WEEK_REPURCHASE_NUM = "WEEK_REPURCHASE_NUM"; //	市场分析报表-周复购数量基准数
    public static final String MONTH_REPURCHASE_NUM = "MONTH_REPURCHASE_NUM"; //	市场分析报表-月复购数量基准数
    public static final String YEAR_REPURCHASE_NUM = "YEAR_REPURCHASE_NUM"; //	市场分析报表-年复购数量基准数

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

    public static Integer getIntegerValByVarName(String varName){
        String value = VALS_MAP.get(varName);
        if(StringUtils.isEmpty(value)){
            return 0;
        }
        return Integer.parseInt(value);
    }
}
