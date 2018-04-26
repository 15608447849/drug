package com.onek.order;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.ESConstant;
import com.onek.consts.MessageEvent;
import com.onek.context.AppContext;
import com.onek.entity.DelayedBase;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entity.TranTransVO;
import com.onek.entitys.Result;
import com.onek.queue.delay.DelayedHandler;
import com.onek.queue.delay.RedisDelayedHandler;
import com.onek.util.LccOrderUtil;
import com.onek.util.area.AreaEntity;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
import com.onek.util.GLOBALConst;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import elasticsearch.ElasticSearchProvider;
import org.apache.http.client.utils.DateUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.rest.RestStatus;
import redis.util.RedisUtil;
import util.MathUtil;
import util.ModelUtil;
import util.StringUtils;
import util.TimeUtils;

import java.util.*;

import static com.onek.order.TranOrderOptModule.getGoodsArr;

public class PayModule {
    public static final DelayedHandler<DelayedBase> DELIVERY_DELAYED =
            new RedisDelayedHandler<>("_DELIVERY", 24,
                    (d) -> new TranOrderOptModule().delivery(d.getOrderNo(), d.getCompid()),
                    DelayedHandler.TIME_TYPE.HOUR);

    public static final String PAY_TYPE_ALI = "alipay";
    public static final String PAY_TYPE_WX = "wxpay";

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_TO_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ? and ostatus=?";

    private static final String GET_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno,orderno,IFNULL(address,'') address,pdnum,IFNULL(consignee,'') consignee,IFNULL(contact,'') contact from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ?";

    private static final String UPDATE_SKU_SALES = "update {{?" + DSMConst.TD_PROD_SKU + "}} set sales = sales + ? where sku = ?";

    private static final String QUERY_ORDER_GOODS = "select g.pdno,g.pnum from {{?" + DSMConst.TD_TRAN_ORDER + "}} o,{{?" + DSMConst.TD_TRAN_GOODS + "}} g" +
            " where g.orderno = o.orderno and o.orderno = ? and o.cusno = ?";

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

            double freight = result[0].getFreight() > 0 ? MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue() : 0;
            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();

            JSONObject r = new JSONObject();
            r.put("payamt", MathUtil.exactAdd(payamt, freight));
            r.put("odate", result[0].getOdate());
            r.put("otime", result[0].getOtime());
            r.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));

            return  new Result().success(r);
        }else{
            return new Result().fail("未查到【"+orderno+"】支付的订单!");
        }

    }

    @UserPermission(ignore = true)
    public Result showFeePayInfo(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate", "otime","pdamt","freight","coupamt","distamt","rvaddno"});

            double payamt = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();

            JSONObject r = new JSONObject();
            r.put("payamt", payamt);
            r.put("afsano", GenIdUtil.getAsOrderId());

            return  new Result().success(r);
        }else{
            return new Result().fail("未查到【"+orderno+"】支付运费的订单!");
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
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt", "odate", "otime"});

            double freight = result[0].getFreight() > 0 ? MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue() : 0;
            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();
            double money = MathUtil.exactAdd(payamt, freight).doubleValue();

            try{
                String r = FileServerUtils.getPayQrImageLink(paytype, "空间折叠", money, orderno,
                        "orderServer" + getOrderServerNo(compid), "PayModule", "payCallBack", compid + "");

                return new Result().success(r);
            }catch (Exception e){
                e.printStackTrace();
                return new Result().fail("生成支付二维码图片失败!");
            }


        }else{
            return new Result().fail("未查到【"+orderno+"】支付的订单!");
        }

    }

    @UserPermission(ignore = true)
    public Result preFeePay(AppContext appContext){

        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        String afsano = jsonObject.get("afsano").getAsString();
        int compid = jsonObject.get("compid").getAsInt();
        String paytype = jsonObject.get("paytype").getAsString();
        if(StringUtils.isEmpty(afsano) || compid <=0){
            return new Result().fail("获取售后单号或企业码失败!");
        }
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
        if(list != null && list.size() > 0){
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate", "otime","pdamt","freight","coupamt","distamt","rvaddno"});

            double payamt = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();

            try{
                String r = FileServerUtils.getPayQrImageLink(paytype, "空间折叠", payamt, afsano,
                        "orderServer" + getOrderServerNo(compid), "PayModule", "payFeeCallBack", compid + "");

                return new Result().success(r);
            }catch (Exception e){
                e.printStackTrace();
                return new Result().fail("生成支付二维码图片失败!");
            }


        }else{
            return new Result().fail("未查到【"+orderno+"】支付运费的订单!");
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
        if("1".equals(tradeStatus)) {
            new GenLccOrderThread(compid, orderno).start();
            new UpdateSalesThread(compid, orderno).start();
        }
        if(result){
            return new Result().success(null);
        }else{
            return new Result().fail(null);
        }

    }

    @UserPermission(ignore = true)
    public Result payFeeCallBack(AppContext appContext){

        String[] arrays = appContext.param.arrays;
        String afsano = arrays[0];
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
            result = successFeeOpt(afsano, paychannel, thirdPayNo, tradeStatus, tdate, time, compid, money);

        }else if("2".equals(tradeStatus)){
            String tdate = TimeUtils.date_yMd_2String(date);
            String time = TimeUtils.date_Hms_2String(date);
            result = failOpt(afsano, paychannel, thirdPayNo, tradeStatus, tdate, time, compid, money);
        }

        new SendMsgThread(afsano, tradeStatus ,money, compid, tradeDate).start();

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
                    jsonResult.put("address",IceRemoteUtil.getCompleteName(result[0].getRvaddno()+""));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                jsonResult.put("address", "");
            }
        }

        return new Result().success(jsonResult);
    }

    @UserPermission(ignore = true)
    public Result getFeePayResult(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        JSONObject jsonResult = new JSONObject();

        List<Object[]> trans = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TRAN_TRANS_SQL, new Object[]{orderno, compid});
        if (trans != null && trans.size() > 0) {
            TranTransVO[] tranTransVOS = new TranTransVO[trans.size()];
            baseDao.convToEntity(trans, tranTransVOS, TranTransVO.class,
                    new String[]{"payprice", "payway", "payno", "orderno", "paysource", "paystatus", "paydate", "paytime", "completedate", "completetime"});
            jsonResult.put("paystatus", tranTransVOS[0].getPaystatus());
        } else {
            jsonResult.put("paystatus", 0);
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
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0, MathUtil.exactMul(price, 100), paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
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

            DELIVERY_DELAYED.add(new DelayedBase(compid, orderno));
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
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0,  MathUtil.exactMul(price, 100), paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
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

    /* *
     * @description 运费支付成功操作
     * @params [arrays]
     * @return boolean
     * @exception
     * @author jiangwg
     * @time  2019/4/25 15:56
     * @version 1.1.1
     **/
    private boolean successFeeOpt(String afsano, int paytype,String thirdPayNo,String tradeStatus,String tradeDate,String tradeTime,int compid,double price) {
        int paysource = 0;

        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();

        sqlList.add(INSERT_TRAN_TRANS);//新增交易记录
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, afsano, 0,  MathUtil.exactMul(price, 100), paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
                thirdPayNo,tradeDate,tradeTime,tradeDate,tradeTime,0});
//        + "(unqid,compid,payno,eventdesc,resultdesc,"
//                + "completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_PAYREC);//新增支付记录
        params.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, "{}", "{}", tradeDate, tradeTime});
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid, TimeUtils.getCurrentYear(), sqlNative, params));

        return b;
    }

    /* *
     * @description 支付运费失败操作
     * @params [arrays]
     * @return boolean
     * @exception
     * @author 11842
     * @time  2019/4/18 20:37
     * @version 1.1.1
     **/
    private boolean failFeeOpt(String afsano, int paytype,String thirdPayNo,String tradeStatus,String tradeDate,String tradeTime,int compid,double price) {

        int paysource = 0;

        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
//        + "(unqid,compid,orderno,payno,payprice,payway,paysource,paystatus,"
//                + "payorderno,tppno,paydate,paytime,completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_TRANS);
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, afsano, 0,  MathUtil.exactMul(price, 100), paytype, paysource, tradeStatus, GenIdUtil.getUnqId(),
                thirdPayNo,tradeDate,tradeTime,tradeDate,tradeTime,0});
//        + "(unqid,compid,payno,eventdesc,resultdesc,"
//                + "completedate,completetime,cstatus)"
        sqlList.add(INSERT_TRAN_PAYREC);
        params.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, "{}", "{}", tradeDate, tradeTime});
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        return !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,TimeUtils.getCurrentYear(), sqlNative, params));
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

    class GenLccOrderThread extends  Thread{
        private int compid;
        private String orderno;

        public GenLccOrderThread(int compid, String orderno){
            this.compid = compid;
            this.orderno = orderno;
        }


        public void run(){

            System.out.println("## 541 line "+compid+ ";"+orderno+" ####");
            List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_PAY_SQL, new Object[]{ orderno, compid});
            if(list != null && list.size() > 0) {
                TranOrder[] result = new TranOrder[list.size()];
                BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt", "odate", "otime", "pdamt", "freight", "coupamt", "distamt", "rvaddno", "orderno", "address","pdnum","consignee","contact"});

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
                    arriarc = "10" + String.valueOf(tranOrder.getRvaddno()).substring(0,7);
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
            }

        }
    }

    class UpdateSalesThread extends  Thread{

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
                        BaseDAO.getBaseDAO().updateNative(UPDATE_SKU_SALES, new Object[]{ sales, sku});
                    }
                }

                try{
                    RedisOrderUtil.addOrderNumByCompid(compid);
                }catch (Exception e){}
            }

        }
    }

    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }
}
