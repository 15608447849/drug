package com.onek.discount.timer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.onek.util.IceRemoteUtil;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import util.MathUtil;
import util.TimeUtils;

import java.math.BigDecimal;
import java.util.*;

public class TeamBuyTask extends TimerTask {

    private static final String SQL = "select a.unqid,a.sdate,a.edate from {{?"+ DSMConst.TD_PROM_ACT+"}} a " +
            "where a.edate = ? and a.brulecode = 1133 and a.cstatus &1 = 0";

    private static String TEAM_BUY_LADOFF_SQL = "select ladamt,ladnum,offer from " +
            "{{?" + DSMConst.TD_PROM_RELA + "}} r, {{?" + DSMConst.TD_PROM_LADOFF + "}} l where r.ladid = l.unqid and l.offercode like '1133%' and r.actcode = ?";

    private static final String UPDATE_COMP_BAL = "update {{?" + DSMConst.TB_COMP + "}} "
            + "set balance = balance + ? where cid = ? ";

    @Override
    public void run() {

        LogUtil.getDefaultLogger().info("++++++ TeamBuyTask execute start +++++++");
        Date date = TimeUtils.addDay(new Date(), -1);
        String y = TimeUtils.date_yMd_2String(date);
        List<Object[]> results = BaseDAO.getBaseDAO().queryNative(SQL, y);
        if(results != null && results.size() > 0){
            for(Object[] obj : results){
                String actCode = obj[0].toString();
                String sdate = obj[1].toString();
                String edate = obj[2].toString();

                LogUtil.getDefaultLogger().info("++++++ TeamBuyTask execute actCode:["+actCode+"]; sdate:["+sdate+"]; edate:["+edate+"] +++++++");

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
                    Set<String> compSet = new HashSet<>();
                    for(Object o : array){
                        LinkedTreeMap map = ((LinkedTreeMap) o);
                        String compid = map.get("compid").toString();
                        compSet.add(compid);
                    }
                    for(int i = 0; i < ladOffArray.size() ; i++){
                         JSONObject js = ladOffArray.getJSONObject(i);
                         if(compSet.size() >= js.getInteger("ladnum")){
                             offer = js.getInteger("offer");
                         }
                    }
                    if(offer > 0){
                        Map<Integer, Integer> dataMap = new HashMap<>();
                        List<TeamBuyMsgBody> msgBodyList = new ArrayList<>();
                        for(Object o : array){
                            LinkedTreeMap map = ((LinkedTreeMap) o);
                            String orderno = map.get("orderno").toString();
                            String compid = map.get("compid").toString();
                            String payamt = map.get("payamt").toString();
                            String pnum = map.get("pnum").toString();
                            double f2 = new BigDecimal((float)(offer) /10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            double f1 = new BigDecimal((float)(10 -offer) /10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            int money = (int)(Integer.parseInt(payamt) * f2);
                            int minusmoney = (int)(Integer.parseInt(payamt) * f1);
                            msgBodyList.add(new TeamBuyMsgBody(orderno, compid, pnum, money, minusmoney));
                            int result = BaseDAO.getBaseDAO().updateNative(UPDATE_COMP_BAL, money, compid);
                            LogUtil.getDefaultLogger().info("++++++ TeamBuyTask compid:["+ compid+"]; money:["+ money+"] result:["+ result+"] +++++++");
                            if(dataMap.containsKey(Integer.parseInt(compid))){
                                money += dataMap.get(Integer.parseInt(compid));
                            }
                            dataMap.put(Integer.parseInt(compid), (int)money);
                        }

                        for(Integer compid : dataMap.keySet()){
                            IceRemoteUtil.insertBalCoup(compid, dataMap.get(compid));
                        }

                        new SendMsgThread(msgBodyList).start();
                    }

                }
            }
        }
        LogUtil.getDefaultLogger().info("++++++ TeamBuyTask execute end +++++++");

    }

    class TeamBuyMsgBody{
        String orderno;
        String compid;
        String pnum;
        double endprice;
        double minusprice;

        public TeamBuyMsgBody(String orderno, String compid, String pnum, double endprice, double minusprice) {
            this.orderno = orderno;
            this.compid = compid;
            this.pnum = pnum;
            this.endprice = endprice;
            this.minusprice = minusprice;
        }

        public String getOrderno() {
            return orderno;
        }

        public void setOrderno(String orderno) {
            this.orderno = orderno;
        }

        public String getCompid() {
            return compid;
        }

        public void setCompid(String compid) {
            this.compid = compid;
        }

        public String getPnum() {
            return pnum;
        }

        public void setPnum(String pnum) {
            this.pnum = pnum;
        }

        public double getEndprice() {
            return endprice;
        }

        public void setEndprice(double endprice) {
            this.endprice = endprice;
        }

        public double getMinusprice() {
            return minusprice;
        }

        public void setMinusprice(double minusprice) {
            this.minusprice = minusprice;
        }
    }

    /**
     * 发送团购消息线程
     */
    class SendMsgThread extends Thread{

        List<TeamBuyMsgBody> msgBodyList;

        public SendMsgThread(List<TeamBuyMsgBody> msgBodyList){
            this.msgBodyList = msgBodyList;
        }

        @Override
        public void run() {
            LogUtil.getDefaultLogger().info("++++++ TeamBuyTask sendMsg start +++++++");

            if(msgBodyList != null && msgBodyList.size() > 0){
                for(TeamBuyMsgBody body : msgBodyList){
                    double endp = MathUtil.exactDiv(body.getEndprice(), 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    double minusp = MathUtil.exactDiv(body.getMinusprice(), 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    try{
                        IceRemoteUtil.sendMessageToClient(Integer.parseInt(body.getCompid()), SmsTempNo.genPushMessageBySystemTemp(SmsTempNo.GROUP_BUYING_END, body.getOrderno() ,String.valueOf(endp), body.getPnum() , String.valueOf(minusp)));
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                    try{
                        String phone = IceRemoteUtil.getSpecifyStorePhone(Integer.parseInt(body.getCompid()));
                        SmsUtil.sendSmsBySystemTemp(phone, SmsTempNo.GROUP_BUYING_END,body.getOrderno(),String.valueOf(endp), body.getPnum() , String.valueOf(minusp));
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }
            }
            LogUtil.getDefaultLogger().info("++++++ TeamBuyTask sendMsg end +++++++");
        }
    }

}
