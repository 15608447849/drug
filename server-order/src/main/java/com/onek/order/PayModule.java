package com.onek.order;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.MessageEvent;
import com.onek.context.AppContext;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderDetail;
import com.onek.entity.TranOrderGoods;
import com.onek.entity.TranTransVO;
import com.onek.entitys.Result;
import com.onek.util.fs.FileServerUtils;
import constant.DSMConst;
import dao.BaseDAO;
import global.GLOBALConst;
import global.GenIdUtil;
import global.IceRemoteUtil;
import util.MathUtil;
import util.ModelUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.onek.order.TranOrderOptModule.getGoodsArr;

public class PayModule {

    public static final String PAY_TYPE_ALI = "alipay";
    public static final String PAY_TYPE_WX = "wxpay";

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_TO_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ? and ostatus=?";

    private static final String GET_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ?";

    private static final String GET_TRAN_TRANS_SQL = "select payprice,payway,payno,orderno,paysource,paystatus,paydate,paytime,completedate,completetime from {{?" + DSMConst.TD_TRAN_TRANS + "}} where orderno=? and compid = ? order by completedate desc,completetime desc limit 1";

    //支付回调更新订单状态
    private static final String UPD_ORDER_STATUS = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?,settstatus=?,"
            + "settdate=?,setttime=? where cstatus&1=0 and orderno=? and ostatus=? ";

    //释放商品冻结库存
    private static final String UPD_GOODS_STORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "store=store-?, freezestore=freezestore-? where cstatus&1=0 and sku=? ";


    //更新活动库存
    private static final String UPD_ACT_STORE = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set "
            + "actstock=actstock-? where cstatus&1=0 and gcode=? and actcode=?";

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

    @UserPermission(ignore = true)
    public Result showPayInfo(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TO_PAY_SQL, new Object[]{ orderno, compid, 0});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt", "odate", "otime"});

            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();

            JSONObject r = new JSONObject();
            r.put("payamt", payamt);
            r.put("odate", result[0].getOdate());
            r.put("otime", result[0].getOtime());
            r.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));

            return  new Result().success(r);
        }else{
            return new Result().fail("未查到【"+orderno+"】支付的订单!");
        }

    }

    @UserPermission(ignore = true)
    public Result prePay(AppContext appContext){

        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        String paytype = jsonObject.get("paytype").getAsString();
        if(StringUtils.isEmpty(orderno) || compid <=0){
            return new Result().fail("获取订单号或企业码失败!");
        }
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TO_PAY_SQL, new Object[]{ orderno, compid, 0});
        if(list != null && list.size() > 0){
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{ "payamt","odate","otime"});

            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();

            String r = FileServerUtils.getPayQrImageLink(paytype, "空间折叠", payamt, orderno,
                    "orderServer" + getOrderServerNo(compid), "PayModule", "payCallBack", compid + "");

            return new Result().success(r);

        }else{
            return new Result().fail("未查到【"+orderno+"】支付的订单!");
        }

    }

    @UserPermission(ignore = true)
    public Result payCallBack(AppContext appContext){

        String[] arrays = appContext.param.arrays;
        String orderno = arrays[0];
        String paytype = arrays[1];
        String thirdPayNo = arrays[2];
        String tradeStatus = arrays[3];
        String tradeDate = arrays[4];
        double money = Double.parseDouble(arrays[5]);
        int compid = Integer.parseInt(arrays[6]);

        int paychannel = -1;
        if(PAY_TYPE_ALI.equals(paytype)){
            paychannel = 2;
        }else if(PAY_TYPE_WX.equals(paytype)){
            paychannel = 1;
        }
        Date date = TimeUtils.str_yMd_Hms_2Date(tradeDate);
        boolean result = false;
        if("1".equals(tradeStatus)){
            String tdate = TimeUtils.date_yMd_2String(date);
            String time = TimeUtils.date_Hms_2String(date);
            result = successOpt(orderno, paychannel, thirdPayNo, tradeStatus, tdate, time, compid, money);
        }else if("2".equals(tradeStatus)){
            String tdate = TimeUtils.date_yMd_2String(date);
            String time = TimeUtils.date_Hms_2String(date);
            result = failOpt(orderno, paychannel, thirdPayNo, tradeStatus, tdate, time, compid, money);
        }

        new SendMsgThread(orderno, tradeStatus ,money, compid, tradeDate).start();

        if(result){
            return new Result().success(null);
        }else{
            return new Result().fail(null);
        }

    }

    @UserPermission(ignore = true)
    public Result getPayResult(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        JSONObject jsonResult = new JSONObject();
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            baseDao.convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate","otime","pdamt","freight", "coupamt","distamt","rvaddno"});

            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();
            double pdamt = MathUtil.exactDiv(result[0].getPdamt(), 100).doubleValue();
            double freight = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();
            double coupamt = MathUtil.exactDiv(result[0].getCoupamt(), 100).doubleValue();
            double distamt = MathUtil.exactDiv(result[0].getDistamt(), 100).doubleValue();

            jsonResult.put("payamt", payamt);
            jsonResult.put("odate", result[0].getOdate());
            jsonResult.put("otime", result[0].getOtime());
            jsonResult.put("pdamt", pdamt);
            jsonResult.put("freight", freight);
            jsonResult.put("coupamt", coupamt);
            jsonResult.put("distamt", distamt);

            List<Object[]> trans = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TRAN_TRANS_SQL, new Object[]{ orderno, compid});
            if(trans != null && trans.size() > 0){
                TranTransVO[] tranTransVOS = new TranTransVO[trans.size()];
                baseDao.convToEntity(trans, tranTransVOS, TranTransVO.class,
                        new String[]{"payprice", "payway", "payno", "orderno", "paysource", "paystatus", "paydate", "paytime", "completedate", "completetime"});
                jsonResult.put("paystatus", tranTransVOS[0].getPaystatus());
            }else{
                jsonResult.put("paystatus", 0);
            }
            if(result[0].getRvaddno() > 0){
                try{
                    jsonResult.put("address",IceRemoteUtil.getArean(result[0].getRvaddno()));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        return new Result().success(jsonResult);
    }

    /* *
     * @description 支付成功操作
     * @params [arrays]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/4/18 20:56
     * @version 1.1.1
     **/
    private boolean successOpt(String orderno, int paytype,String thirdPayNo,String tradeStatus,String tradeDate,String tradeTime,int compid,double price) {
        int paysource = 0;

        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        List<Object[]> paramsOne = new ArrayList<>();
        List<Object[]> paramsTwo = new ArrayList<>();
        sqlList.add(UPD_ORDER_STATUS);//更新订单状态
        params.add(new Object[]{1,1,tradeDate,tradeTime,orderno,0});

        sqlList.add(INSERT_TRAN_TRANS);//新增交易记录
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0, price, paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
                thirdPayNo,tradeDate,tradeTime,tradeDate,tradeTime,0});
//        + "(unqid,compid,payno,eventdesc,resultdesc,"
//                + "completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_PAYREC);//新增支付记录
        params.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, "{}", "{}", tradeDate, tradeTime});
        int year = Integer.parseInt("20" + orderno.substring(0,2));
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,year, sqlNative, params));
        if (b) {
            TranOrderGoods[] tranOrderGoods = getGoodsArr(orderno, compid);
            for (TranOrderGoods tranOrderGood : tranOrderGoods) {
                paramsOne.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPnum(), tranOrderGood.getPdno()});
                paramsTwo.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno(),tranOrderGood.getActCode()});
            }
            baseDao.updateBatchNative(UPD_GOODS_STORE, paramsOne, tranOrderGoods.length);//更新商品库存(若 失败  异常处理)
            //更新活动库存
            baseDao.updateBatchNative(UPD_ACT_STORE, paramsTwo, tranOrderGoods.length);
        }
        return b;
    }

    /* *
     * @description 支付失败操作
     * @params [arrays]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/4/18 20:37
     * @version 1.1.1
     **/
    private boolean failOpt(String orderno, int paytype,String thirdPayNo,String tradeStatus,String tradeDate,String tradeTime,int compid,double price) {

        int paysource = 0;

        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
//        + "(unqid,compid,orderno,payno,payprice,payway,paysource,paystatus,"
//                + "payorderno,tppno,paydate,paytime,completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_TRANS);
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0, price, paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
                thirdPayNo,tradeDate,tradeTime,tradeDate,tradeTime,0});
//        + "(unqid,compid,payno,eventdesc,resultdesc,"
//                + "completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_PAYREC);
        params.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, "{}", "{}", tradeDate, tradeTime});
        int year = Integer.parseInt("20" + orderno.substring(0,2));
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,year, sqlNative, params));
    }

    class SendMsgThread extends Thread{
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
            JSONObject jsonObject = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("orderNo", orderno);
            body.put("tradeStatus", tradeStatus);
            body.put("money", money);
            body.put("compid", compid);
            body.put("tradeDate", tradeDate);
            jsonObject.put("event", MessageEvent.PAY_CALLBACK.getState());
            jsonObject.put("body", body);
            IceRemoteUtil.sendMessageToClient(compid, jsonObject.toJSONString());

        }
    }

    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }
}
