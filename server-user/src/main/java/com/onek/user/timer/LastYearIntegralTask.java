package com.onek.user.timer;

import com.alibaba.fastjson.JSONObject;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.util.*;

public class DiscountRuleTask extends TimerTask {

    private static final String SQL = "select d.gcode,d.actcode,a.brulecode,d.cstatus from {{?"+ DSMConst.TD_PROM_ASSDRUG +"}} d " +
            "left join {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "on d.actcode = a.unqid " +
            "where edate = ? ";

    @Override
    public void run() {
        Date date = TimeUtils.addDay(new Date(), -1);
        String y = TimeUtils.date_yMd_2String(date);
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SQL, y);
        if(results != null && results.size() > 0){
            ActivityManageServer server = new ActivityManageServer();
            server.registerObserver(new ProdDiscountObserver());

            Map<Integer,List<String>> map = new HashMap<>();
            for(Object[] result : results){
                Long gcode = (Long) result[0];
//                String actcode = (String) result[1];
                int rulecode = (Integer) result[2];
                int cstatus = (Integer) result[3];

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("discount", "1");
                jsonObject.put("gcode",  gcode);
                jsonObject.put("cstatus", cstatus);
                jsonObject.put("rulecode", rulecode);

                List<String> list = map.get(rulecode);
                if(list == null || list.size() <=0){
                    list = new ArrayList<>();
                }
                list.add(jsonObject.toJSONString());
                map.put(rulecode, list);
            }

            for(Integer rulecode : map.keySet()){
                server.setProd(map.get(rulecode));
            }
        }
    }
}
