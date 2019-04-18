package com.onek.goods.service;

import constant.DSMConst;
import dao.BaseDAO;

import java.util.ArrayList;
import java.util.List;

public class PromTimeService {

    private static String PROM_TIME_SQL = "select sdate,edate from {{?"+ DSMConst.TD_PROM_TIME +"}} where cstatus&1=0 and actcode = ?";

    private static final BaseDAO BASE_DAO = BaseDAO.getBaseDAO();

    public List<String[]> getTimesByActcode(long actcode){
        List<Object[]> list2= BASE_DAO.queryNative(PROM_TIME_SQL, new Object[]{ actcode });
        List<String[]> times = new ArrayList<>();
        for(Object[] objects1 : list2){
            String sdate =  objects1[0].toString();
            String edate =  objects1[1].toString();
            times.add(new String[]{ sdate, edate});
        }
        if(times == null || times.size() <=0){
            times.add(new String[]{ "00:00:00",  "23:59:59"});
        }
        return times;
    }
}
