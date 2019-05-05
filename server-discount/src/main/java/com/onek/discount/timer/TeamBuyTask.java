package com.onek.discount.timer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.onek.propagation.prod.ActivityManageServer;
import com.onek.propagation.prod.ProdDiscountObserver;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.TimeUtils;

import java.math.BigDecimal;
import java.util.*;

import static Ice.Application.communicator;

public class TeamBuyTask extends TimerTask {

    private static final String SQL = "select a.unqid,a.sdate,a.edate from {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "where a.edate = ? and a.brulecode = 1133 and a.cstatus &1 = 0";

    private static String TEAM_BUY_LADOFF_SQL = "select ladamt,ladnum,offer from " +
            "{{?" + DSMConst.TD_PROM_RELA + "}} r, {{?" + DSMConst.TD_PROM_LADOFF + "}} l where r.ladid = l.unqid and l.offercode like '1133%' and r.actcode = ?";

    private static final String UPDATE_COMP_BAL = "update {{?" + DSMConst.D_COMP + "}} "
            + "set balance = balance + ? where cid = ? ";

    @Override
    public void run() {
        System.out.println("++++++ TeamBuyTask execute start +++++++");
        Date date = TimeUtils.addDay(new Date(), -1);
        String y = TimeUtils.date_yMd_2String(date);
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SQL, y);
        if(results != null && results.size() > 0){
            for(Object[] obj : results){
                String actCode = obj[0].toString();
                String sdate = obj[1].toString();
                String edate = obj[2].toString();

                System.out.println("++++++ TeamBuyTask execute actCode:["+actCode+"]; sdate:["+sdate+"]; edate:["+edate+"] +++++++");

                List<Object[]> ladOffList = BaseDAO.getBaseDAO().queryNative(TEAM_BUY_LADOFF_SQL, new Object[]{actCode});
                JSONArray ladOffArray = new JSONArray();
                if (ladOffList != null && ladOffList.size() > 0) {
                    for (Object[] objects : ladOffList) {
                        int amt = Integer.parseInt(objects[0].toString());
                        int num = Integer.parseInt(objects[1].toString());
                        int offer = Integer.parseInt(objects[2].toString()) / 100;
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("ladamt", amt);
                        jsonObject.put("ladnum", num);
                        jsonObject.put("offer", offer);
                        ladOffArray.add(jsonObject);
                    }
                }

                ArrayList array = IceRemoteUtil.queryTeamBuyOrder(sdate,edate, actCode);
                if(array != null && array.size() > 0){

                    int offer = 0;
                    for(int i = 0; i < ladOffArray.size() ; i++){
                         JSONObject js = ladOffArray.getJSONObject(i);
                         if(array.size() >= js.getInteger("ladnum")){
                             offer = js.getInteger("offer");
                         }
                    }
                    if(offer > 0){
                        for(Object o : array){
                            LinkedTreeMap map = ((LinkedTreeMap) o);
                            String compid = map.get("compid").toString();
                            String payamt = map.get("payamt").toString();
                            double f1 = new BigDecimal((float)(100 -offer) /100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            double money = Integer.parseInt(payamt) * f1;
                            System.out.println("++++++ TeamBuyTask compid:["+ compid+"]; money:["+ money+"] +++++++");
                            int result = BaseDAO.getBaseDAO().updateNative(UPDATE_COMP_BAL, money, compid);
                            System.out.println("++++++ TeamBuyTask compid:["+ compid+"]; result:["+ result+"] +++++++");
                        }
                    }

                }
            }
        }
        System.out.println("++++++ TeamBuyTask execute end +++++++");
    }
}
