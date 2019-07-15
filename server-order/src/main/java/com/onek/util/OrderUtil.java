package com.onek.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hsf.framework.api.driver.DriverServicePrx;
import com.hsf.framework.api.myOrder.MyOrderServerPrx;
import com.onek.consts.ESConstant;
import com.onek.consts.IntegralConstant;
import com.onek.consts.MessageEvent;
import com.onek.entity.TranOrder;
import com.onek.entity.TranTransVO;
import com.onek.order.OrderOptModule;
import com.onek.property.LccProperties;
import com.onek.util.area.AreaEntity;
import com.onek.util.member.MemberStore;
import constant.DSMConst;
import dao.BaseDAO;
import elasticsearch.ElasticSearchProvider;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.rest.RestStatus;
import util.MathUtil;
import util.ModelUtil;
import util.StringUtils;
import util.TimeUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderUtil {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    public static final String PAY_TYPE_ALI = "alipay";
    public static final String PAY_TYPE_WX = "wxpay";

    private static String INSERT_INTEGRAL_DETAIL_SQL = "insert into {{?"+ DSMConst.TD_INTEGRAL_DETAIL + "}} " +
            "(unqid,compid,istatus,integral,busid,createdate,createtime,cstatus) values(?,?,?,?,?,CURRENT_DATE,CURRENT_TIME,?)";

    private static final String GET_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno,balamt,orderno,IFNULL(address,'') address,pdnum,IFNULL(consignee,'') consignee,IFNULL(contact,'') contact from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ?";

    //远程调用
    private static final String INSERT_PROM_GROUP = "insert into {{?" + DSMConst.TD_PROM_GROUP + "}} (unqid,actcode,compid,joindate,jointime) values(?,?,?,CURRENT_DATE,CURRENT_TIME) ";

    private static final String QUERY_ORDER_GOODS = "select g.pdno,g.pnum,g.promtype,g.actcode from {{?" + DSMConst.TD_TRAN_ORDER + "}} o,{{?" + DSMConst.TD_TRAN_GOODS + "}} g" +
            " where g.orderno = o.orderno and o.orderno = ? and o.cusno = ?";
    //远程调用
    private static final String UPDATE_SKU_SALES = "update {{?" + DSMConst.TD_PROD_SKU + "}} set sales = sales + ? where sku = ?";

    //订单交易表新增
    private static final String INSERT_TRAN_TRANS = "insert into {{?" + DSMConst.TD_TRAN_TRANS + "}} "
            + "(unqid,compid,orderno,payno,payprice,payway,paysource,paystatus,"
            + "payorderno,tppno,paydate,paytime,completedate,completetime,cstatus)"
            + " values(?,?,?,?,?,"
            + "?,?,?,?,?,"
            + "?,?,?,?,?)";

    //支付记录
    private static final String INSERT_TRAN_PAYREC = "insert into {{?" + DSMConst.TD_TRAN_PAYREC + "}} "
            + "(unqid,compid,payno,eventdesc,resultdesc,"
            + "completedate,completetime,cstatus)"
            + " values(?,?,?,?,?,"
            + "?,?,0)";

    // 查询订单交易表
    private static final String SELECT_TRAN_TRANS = "select unqid,compid,orderno,payno,payprice,payway,paysource,paystatus," +
            "payorderno,tppno,paydate,paytime,completedate,completetime,cstatus  from {{?" + DSMConst.TD_TRAN_TRANS + "}} where cstatus&1=0 and orderno=? and compid=? and paystatus=1 and payway in (1,2) " +
            "order by completedate desc,completetime desc";

    /**
     * 支付成功调用发送消息
     *
     * @param orderno
     * @param tradeStatus
     * @param money
     * @param compid
     * @param tradeDate
     */
    public static void sendMsg(String orderno, String tradeStatus,double money,int compid,String tradeDate){
        new SendMsgThread(orderno, tradeStatus, money, compid, tradeDate).start();
    }

    /**
     * 产生一块物流订单
     *
     * @param compid
     * @param orderno
     */
    public static void generateLccOrder(int compid,String orderno){
        new GenLccOrderThread(compid, orderno).start();
    }

    /**
     * 更新销量
     *
     * @param compid
     * @param orderno
     */
    public static void updateSales(int compid,String orderno){
        new UpdateSalesThread(compid, orderno).start();
    }

    public static Map<String,String> refund(int compid, String orderno){

        int paysource = 0;
        Map<String,String> map = new HashMap<>();
        boolean success = false;
        String message = "";
        List<Object[]> queryList = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), SELECT_TRAN_TRANS, new Object[]{orderno, compid});
        if(queryList != null && queryList.size() > 0){
            TranTransVO [] transVOS = new TranTransVO[queryList.size()];
            baseDao.convToEntity(queryList, transVOS, TranTransVO.class, new String[]{
                    "unqid","compid","orderno","payno","payprice","payway","paysource","paystatus",
                            "payorderno","tppno","paydate","paytime","completedate","completetime","cstatus"});
            String tppno = transVOS[0].getTppno(); // 第三方支付流水
            int payWay = transVOS[0].getPayway();
            double payprice = transVOS[0].getPayprice();
            double prize = MathUtil.exactDiv(payprice, 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            long refundno = GenIdUtil.getUnqId();
            if(payWay == 1 || payWay == 2){ // 支付宝、微信
                String type = payWay == 1 ? PAY_TYPE_WX : PAY_TYPE_ALI;
                HashMap<String,Object> resultMap = FileServerUtils.refund(type, refundno + "", tppno, prize, prize, false);
                Double code = (Double) resultMap.get("code");
                String tradeStatus = (code.intValue())+"";
//                if(code == 10000){
//                    tradeStatus = "-3";
//                }else{
//                    tradeStatus = "-2";
//                }
                message = resultMap.get("message").toString();
                List<String> sqlList = new ArrayList<>();
                List<Object[]> params = new ArrayList<>();
                sqlList.add(INSERT_TRAN_TRANS);
                params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, refundno,  transVOS[0].getPayprice(), type, paysource, tradeStatus, GenIdUtil.getUnqId(),
                        0, TimeUtils.getCurrentDate(), TimeUtils.getCurrentTime(),TimeUtils.getCurrentDate(), TimeUtils.getCurrentTime(),0});
                sqlList.add(INSERT_TRAN_PAYREC);
                params.add(new Object[]{GenIdUtil.getUnqId(),compid, refundno, "{}", "{}", TimeUtils.getCurrentDate(), TimeUtils.getCurrentTime()});
                int year = Integer.parseInt("20" + orderno.substring(0,2));
                String[] sqlNative = new String[sqlList.size()];
                sqlNative = sqlList.toArray(sqlNative);
                success = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,year, sqlNative, params));

            }
        }
        map.put("result", success ? "1": "0");
        map.put("message", message);
        return map;
    }

    static class SendMsgThread extends Thread{
        private String orderno;
        private String tradeStatus;
        private double money;
        private int compid;
        private String tradeDate;

        public SendMsgThread(String orderno, String tradeStatus, double money, int compid, String tradeDate){
            this.orderno = orderno;
            this.tradeStatus = tradeStatus;
            this.money = money;
            this.compid = compid;
            this.tradeDate = tradeDate;
        }

        @Override
        public void run() {
            JSONObject body = new JSONObject();
                body.put("orderNo", orderno);
                body.put("tradeStatus", tradeStatus);
                body.put("money", money);
                body.put("compid", compid);
                body.put("tradeDate", tradeDate);
            JSONObject jsonObject = new JSONObject();
                jsonObject.put("event", MessageEvent.PAY_CALLBACK.getState());
                jsonObject.put("body", body);
             String msg = "pay:" + jsonObject.toJSONString();
            IceRemoteUtil.sendMessageToClient(compid, msg);
            if(orderno.length() >= 16 &&  "1".equals(tradeStatus)){ // 交易成功
                try{
                    IceRemoteUtil.sendMessageToClient(compid, SmsTempNo.genPushMessageBySystemTemp(SmsTempNo.ORDER_PAYMENT_SUCCESSFUL,orderno));
                }catch(Exception e){
                    e.printStackTrace();
                }
                try{
                    String phone = IceRemoteUtil.getSpecifyStorePhone(compid);
                    SmsUtil.sendSmsBySystemTemp(phone, SmsTempNo.ORDER_PAYMENT_SUCCESSFUL,orderno);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    static class GenLccOrderThread extends  Thread{
        private int compid;
        private String orderno;

        public GenLccOrderThread(int compid, String orderno){
            this.compid = compid;
            this.orderno = orderno;
        }


        public void run(){
            //生成节点信息一
            try {
                OrderOptModule.generateNode(orderno + "", compid, OrderOptModule.generateNodeObj(1));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("## 541 line "+compid+ ";"+orderno+" ####");
            List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
            if(list != null && list.size() > 0) {
                TranOrder[] result = new TranOrder[list.size()];
                BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt", "odate", "otime", "pdamt", "freight", "coupamt", "distamt", "rvaddno","balamt", "orderno", "address","pdnum","consignee","contact"});

                TranOrder tranOrder = result[0];

                String arriarc = "";
                AreaEntity[] areaEntities = IceRemoteUtil.getAncestors(tranOrder.getRvaddno());
                if(areaEntities != null && areaEntities.length > 0){
                    for(int i = areaEntities.length - 1; i>=0; i--){
                        if(areaEntities[i] != null && !StringUtils.isEmpty(areaEntities[i].getLcareac()) && Integer.parseInt(areaEntities[i].getLcareac()) > 0){
                            arriarc = areaEntities[i].getLcareac();
                            break;
                        }
                    }
                }

                if(StringUtils.isEmpty(arriarc) && tranOrder.getRvaddno() > 0){
                    arriarc = "10" + String.valueOf(tranOrder.getRvaddno()).substring(0,6);
                }

                if(StringUtils.isEmpty(arriarc)){
                    arriarc = "10";
                }

                String lccResult = LccOrderUtil.addLccOrder(tranOrder, arriarc);
                if(!StringUtils.isEmpty(lccResult)){
                    JSONObject jsonObject = JSONObject.parseObject(lccResult);
                    if(Integer.parseInt(jsonObject.get("code").toString()) == 0){
                        System.out.println("## 生成一块物流订单成功 ####");
                    }else{
                        System.out.println("## 生成一块物流订单失败 ####");
                    }
                }

//                long unqid = GenIdUtil.getUnqId();
//                int payamt = MathUtil.exactDiv(tranOrder.getPayamt(), 100).intValue();
//                int code = MemberStore.addPoint(compid, payamt);
//                if(code > 0) baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),INSERT_INTEGRAL_DETAIL_SQL, new Object[]{unqid, compid, IntegralConstant.SOURCE_ORDER_GIVE, payamt, orderno,0 });

            }

        }
    }

    static class UpdateSalesThread extends  Thread{

        private int compid;
        private String orderno;

        public UpdateSalesThread(int compid, String orderno){
            this.compid = compid;
            this.orderno = orderno;
        }

        public void run(){

            List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), QUERY_ORDER_GOODS, new Object[]{ orderno, compid});
            if(list != null && list.size() > 0) {

                for(Object[] obj : list){
                    long sku = Long.parseLong(obj[0].toString());
                    int sales = Integer.parseInt(obj[1].toString());

                    GetResponse response = ElasticSearchProvider.getDocumentById(ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku +"");
                    boolean updateSuccess = false;
                    if(response != null){
                        Map<String, Object> data = response.getSourceAsMap();
                        int orgsales = data.get(ESConstant.PROD_COLUMN_SALES) != null ? Integer.parseInt(data.get(ESConstant.PROD_COLUMN_SALES).toString()) : 0;
                        data.put(ESConstant.PROD_COLUMN_SALES, sales + orgsales);
                        HashMap detail = (HashMap) data.get("detail");
                        data.put(ESConstant.PROD_COLUMN_DETAIL, JSONObject.toJSON(detail));
                        data.put(ESConstant.PROD_COLUMN_TIME, data.get(ESConstant.PROD_COLUMN_TIME).toString());
                        UpdateResponse updateResponse = ElasticSearchProvider.updateDocumentById(data, ESConstant.PROD_INDEX, ESConstant.PROD_TYPE, sku+"");
                        if(response != null && RestStatus.OK == updateResponse.status()) {
                            updateSuccess = true;
                        }
                    }
                    if(updateSuccess){
                        //远程调用
                        IceRemoteUtil.updateNative(UPDATE_SKU_SALES, sales, sku);
                    }
                }

                for(Object[] obj : list) {
                    int promtype = Integer.parseInt(obj[2].toString());
                    String actCodeStr = obj[3].toString();
                    if((promtype & 4096) > 0 && !StringUtils.isEmpty(actCodeStr)){
                        List<Long> actCodeList = JSON.parseArray(actCodeStr).toJavaList(Long.class);
                        for (Long actCode: actCodeList) {
                            IceRemoteUtil.updateNative(INSERT_PROM_GROUP, GenIdUtil.getUnqId(), actCode, compid);//远程调用
                        }

                    }
                }
            }


            List<Object[]> plist = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
            if(plist != null && plist.size() > 0) {
                TranOrder[] result = new TranOrder[plist.size()];
                BaseDAO.getBaseDAO().convToEntity(plist, result, TranOrder.class, new String[]{"payamt", "odate", "otime", "pdamt", "freight", "coupamt", "distamt", "rvaddno", "balamt", "orderno", "address", "pdnum", "consignee", "contact"});

                TranOrder tranOrder = result[0];

                long unqid = GenIdUtil.getUnqId();
                int payamt = MathUtil.exactDiv(tranOrder.getPayamt(), 100).intValue();
                int code = MemberStore.addPoint(compid, payamt);
                if(code > 0) baseDao.updateNativeSharding(compid, TimeUtils.getCurrentYear(),INSERT_INTEGRAL_DETAIL_SQL, new Object[]{unqid, compid, IntegralConstant.SOURCE_ORDER_GIVE, payamt, orderno,0 });

            }

        }
    }

    public static void changeYKWLOrderState(String orderNo, int state, int compid) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            if (state == 3) {//发货
                OrderOptModule.generateNode(orderNo , compid, OrderOptModule.generateNodeObj(2));
                OrderOptModule.generateNode(orderNo , compid, OrderOptModule.generateNodeObj(3));
            } else {//收货
                OrderOptModule.generateNode(orderNo , compid, OrderOptModule.generateNodeObj(4));
            }
            int pubCompId = LccProperties.INSTANCE.pubercompid;
            MyOrderServerPrx myOrderServer =(MyOrderServerPrx) RpcClientUtil.getServicePrx(MyOrderServerPrx.class);
            String billno = myOrderServer.getOrderNoByCompId(orderNo, pubCompId);
            DriverServicePrx driverServicePrx = (DriverServicePrx)RpcClientUtil.getServicePrx(DriverServicePrx.class);
            driverServicePrx.updateOrderStateYKYY(288, pubCompId, Long.parseLong(billno), state);
        });
    }
}
