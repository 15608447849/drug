package com.onek.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.consts.ESConstant;
import com.onek.consts.IntegralConstant;
import com.onek.consts.MessageEvent;
import com.onek.context.AppContext;
import com.onek.entity.DelayedBase;
import com.onek.entity.TranOrder;
import com.onek.entity.TranOrderGoods;
import com.onek.entity.TranTransVO;
import com.onek.entitys.Result;
import com.onek.queue.delay.DelayedHandler;
import com.onek.queue.delay.RedisDelayedHandler;
import com.onek.util.*;
import com.onek.util.area.AreaEntity;
import com.onek.util.area.AreaFeeUtil;
import com.onek.util.fs.FileServerUtils;
import com.onek.util.member.MemberStore;
import com.onek.util.order.RedisOrderUtil;
import com.onek.util.stock.RedisStockUtil;
import constant.DSMConst;
import dao.BaseDAO;
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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.onek.order.TranOrderOptModule.CANCEL_DELAYED;
import static com.onek.order.TranOrderOptModule.getGoodsArr;

public class PayModule {
    public static final DelayedHandler<DelayedBase> DELIVERY_DELAYED =
            new RedisDelayedHandler<>("_DELIVERY", 24,
                    (d) -> new TranOrderOptModule().delivery(d.getOrderNo(), d.getCompid()),
                    DelayedHandler.TIME_TYPE.HOUR);

    //线下即付变为待发货一小时轮询
    public static final DelayedHandler<DelayedBase> CANCEL_XXJF =
            new RedisDelayedHandler<>("_CANEL_XXJF", 60,
                    (d) -> toBeShipped(d.getOrderNo(), d.getCompid()),
                    DelayedHandler.TIME_TYPE.MINUTES);

    public static final String PAY_TYPE_ALI = "alipay";
    public static final String PAY_TYPE_WX = "wxpay";
    public static final String PAY_TYPE_HDFK = "hdfk";
    public static final String PAY_TYPE_ZZ = "zz";
    public static final String PAY_TYPE_YE = "yepay";

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static final String GET_TO_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno,balamt from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ? and ostatus=?";

    private static final String GET_PAY_SQL = "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno,balamt,orderno,IFNULL(address,'') address,pdnum,IFNULL(consignee,'') consignee,IFNULL(contact,'') contact from {{?" + DSMConst.TD_TRAN_ORDER + "}} where orderno=? and cusno = ?";

    private static final String QUERY_ORDER_GOODS = "select g.pdno,g.pnum,g.promtype,g.actcode from {{?" + DSMConst.TD_TRAN_ORDER + "}} o,{{?" + DSMConst.TD_TRAN_GOODS + "}} g" +
            " where g.orderno = o.orderno and o.orderno = ? and o.cusno = ?";

    private static final String GET_TRAN_TRANS_SQL = "select payprice,payway,payno,orderno,paysource,paystatus,paydate,paytime,completedate,completetime from {{?" + DSMConst.TD_TRAN_TRANS + "}} where orderno=? and compid = ? order by completedate desc,completetime desc limit 1";

    //支付回调更新订单状态
    private static final String UPD_ORDER_STATUS = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?,settstatus=?,"
            + "settdate=?,setttime=?, payway=? where cstatus&1=0 and orderno=? and ostatus=? ";

    //释放商品冻结库存
    private static final String UPD_GOODS_STORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "store=store-?, freezestore=freezestore-? where cstatus&1=0 and sku=? ";

    //更新活动库存
    private static final String UPD_ACT_STORE = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set "
            + "actstock=actstock-? where cstatus&1=0 and gcode=? and actcode=?";


    //取消时加库存
    private static final String ADD_GOODS_STORE = "update {{?" + DSMConst.TD_PROD_SKU + "}} set "
            + "store=store+? where cstatus&1=0 and sku=? ";

    //取消时加活动库存
    private static final String ADD_ACT_STORE = "update {{?" + DSMConst.TD_PROM_ASSDRUG + "}} set "
            + "actstock=actstock+? where cstatus&1=0 and gcode=? and actcode=? ";

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

    private static final String UPD_ASAPP_SQL = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set cstatus=cstatus&~1 "
            + " where cstatus&1>0 and asno=? ";

    @UserPermission(ignore = true)
    public Result showPayInfo(AppContext appContext){
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TO_PAY_SQL,
                new Object[]{ orderno, compid, 0});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate","otime","pdamt","freight","coupamt","distamt","rvaddno","balamt"});
            double bal = MathUtil.exactDiv(result[0].getBalamt(), 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).
                    setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            if(bal == 0 && payamt <= 0 ){
                return new Result().fail("支付金额不能小于0!");
            }

            int bflag = 0;
            if(bal > 0 && payamt == 0){
                bflag = 1;
            }

            JSONObject r = new JSONObject();
            r.put("balflag",bflag);
            r.put("bal",bal);
            r.put("payamt", payamt);
            r.put("odate", result[0].getOdate());
            r.put("otime", result[0].getOtime());
            r.put("now", TimeUtils.date_yMd_Hms_2String(new Date()));

            return  new Result().success(r);
        }else{
            return new Result().fail("该订单已支付!");
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
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate", "otime","pdamt","freight","coupamt","distamt","rvaddno","balamt"});

//            double payamt = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();

            double payamt = AreaFeeUtil.getFee(result[0].getRvaddno());
            if(payamt <= 0){
                return new Result().fail("该地区暂不支持在线开票，请联系客服");
            }

            JSONObject r = new JSONObject();
            r.put("payamt", payamt);
//            r.put("afsano", GenIdUtil.getAsOrderId());

            return  new Result().success(r);
        }else{
            return new Result().fail("运费已支付!");
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
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate","otime","pdamt","freight","coupamt","distamt","rvaddno","balamt"});
            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();
            if(payamt <= 0){
                return new Result().fail("支付金额不能小于0!");
            }
            try{
                String r = FileServerUtils.getPayQrImageLink(paytype, "空间折叠", payamt, orderno,
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
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate", "otime","pdamt","freight","coupamt","distamt","rvaddno","balamt"});

//            double payamt = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();

            double payamt = AreaFeeUtil.getFee(result[0].getRvaddno());
            if(payamt <= 0){
                return new Result().fail("该地区暂不支持在线开票，请联系客服");
            }
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
        }else if(PAY_TYPE_ZZ.equals(paytype)){
            paychannel = 5;
        }else if(PAY_TYPE_HDFK.equals(paytype)){
            paychannel = 4;
        }else if(PAY_TYPE_YE.equals(paytype)){
            paychannel = 0;
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

        OrderUtil.sendMsg(orderno, tradeStatus ,money, compid, tradeDate);
        if("1".equals(tradeStatus)) {
            OrderUtil.generateLccOrder(compid, orderno);
            OrderUtil.updateSales(compid, orderno);
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

        OrderUtil.sendMsg(afsano, tradeStatus ,money, compid, tradeDate);

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
//            "select payamt,odate,otime,pdamt,freight,coupamt,distamt,rvaddno,balamt,orderno," +
//                    "IFNULL(address,'') address,pdnum,IFNULL(consignee,'') consignee,IFNULL(contact,'') contact
            baseDao.convToEntity(list, result, TranOrder.class, new String[]{
                    "payamt","odate","otime","pdamt","freight", "coupamt","distamt","rvaddno","balamt"});

            double payamt = MathUtil.exactDiv(result[0].getPayamt(), 100).doubleValue();
            double pdamt = MathUtil.exactDiv(result[0].getPdamt(), 100).doubleValue();
            double freight = MathUtil.exactDiv(result[0].getFreight(), 100).doubleValue();
            double coupamt = MathUtil.exactDiv(result[0].getCoupamt(), 100).doubleValue();
            double distamt = MathUtil.exactDiv(result[0].getDistamt(), 100).doubleValue();
            double balamt = MathUtil.exactDiv(result[0].getBalamt(), 100).doubleValue();


            jsonResult.put("payamt", payamt);
            jsonResult.put("odate", result[0].getOdate());
            jsonResult.put("otime", result[0].getOtime());
            jsonResult.put("pdamt", pdamt);
            jsonResult.put("freight", freight);
            jsonResult.put("coupamt", coupamt);
            jsonResult.put("distamt", distamt);
            jsonResult.put("balamt", balamt);
            jsonResult.put("consignee", String.valueOf(list.get(0)[12]));
            jsonResult.put("contact", String.valueOf(list.get(0)[13]));

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

        int paySpreadBal = getPaySpreadBal(compid,orderno);
        if(paySpreadBal > 0 && paytype != 0){
            sqlList.add(INSERT_TRAN_TRANS);
            params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0, paySpreadBal, 0, paysource, tradeStatus, GenIdUtil.getUnqId(),
                    thirdPayNo,tradeDate,tradeTime,tradeDate,tradeTime,0});
        }

        sqlList.add(UPD_ORDER_STATUS);//更新订单状态
        params.add(new Object[]{1,1,
                tradeDate,tradeTime,
                paytype,
                orderno,0});

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
            //减数据库库存
            reduceGoodsDbStock(orderno, compid);
            if(paySpreadBal > 0 && paytype != 0){
                IceRemoteUtil.updateCompBal(compid,-paySpreadBal);
            }

            if(paytype == 0){
                IceRemoteUtil.updateCompBal(compid,-(MathUtil.exactMul(price, 100).intValue()));
            }

            if (paytype != 4 && paytype != 5) {
                DELIVERY_DELAYED.add(new DelayedBase(compid, orderno));
            }

            try{
                //满赠赠优惠券
                new CouponRevModule().revGiftCoupon(Long.parseLong(orderno),compid);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return b;
    }

    /* *
     * @description 减数据库库存
     * @params [orderno, compid]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/5/11 15:53
     * @version 1.1.1
     **/
    public static void reduceGoodsDbStock(String orderno, int compid) {
        List<Object[]> paramsOne = new ArrayList<>();
        List<Object[]> paramsTwo = new ArrayList<>();
        TranOrderGoods[] tranOrderGoods = getGoodsArr(orderno, compid);
        for (TranOrderGoods tranOrderGood : tranOrderGoods) {
            paramsOne.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPnum(), tranOrderGood.getPdno()});
            List<Long> list = JSON.parseArray(tranOrderGood.getActcode()).toJavaList(Long.class);
            for (Long aList : list) {
                paramsTwo.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno(), aList});
            }
        }
        baseDao.updateBatchNative(UPD_GOODS_STORE, paramsOne, tranOrderGoods.length);//更新商品库存(若 失败  异常处理)
        //更新活动库存
        baseDao.updateBatchNative(UPD_ACT_STORE, paramsTwo, paramsTwo.size());
    }


    /* *
     * @description 加数据库库存
     * @params [orderno, compid]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/5/11 15:53
     * @version 1.1.1
     **/
    public static void addGoodsDbStock(String orderno, int compid) {
        List<Object[]> paramsOne = new ArrayList<>();
        List<Object[]> paramsTwo = new ArrayList<>();
        TranOrderGoods[] tranOrderGoods = getGoodsArr(orderno, compid);
        for (TranOrderGoods tranOrderGood : tranOrderGoods) {
            paramsOne.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno()});
            List<Long> list = JSON.parseArray(tranOrderGood.getActcode()).toJavaList(Long.class);
            for (Long aList : list) {
                paramsTwo.add(new Object[]{tranOrderGood.getPnum(), tranOrderGood.getPdno(), aList});
            }
        }
        baseDao.updateBatchNative(ADD_GOODS_STORE, paramsOne, tranOrderGoods.length);//更新商品库存(若 失败  异常处理)
        //更新活动库存
        baseDao.updateBatchNative(ADD_ACT_STORE, paramsTwo, paramsTwo.size());
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
        if(b){
            baseDao.updateNativeSharding(0, TimeUtils.getCurrentYear(), UPD_ASAPP_SQL, afsano);
        }
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

    @UserPermission(ignore = false)
    public Result offlinePay(AppContext appContext) {
        int result;
        String json = appContext.param.json;
        JSONObject jsonObject = JSON.parseObject(json);
        String orderno = jsonObject.getString("orderno");
        int year = Integer.parseInt("20" + orderno.substring(0, 2));
        int compid = jsonObject.getIntValue("compid");
        Integer paytype = jsonObject.getInteger("paytype");

        if(!StringUtils.isBiggerZero(orderno) || compid <= 0){
            return new Result().fail("获取订单号或企业码失败!");
        }
        String updateSQL =  "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=?,"
                + "odate=CURRENT_DATE,otime=CURRENT_TIME, payway=? where cstatus&1=0 and orderno=? and ostatus=? "
                + " and payway=-1";
        //线下到付、线下即付订单状态改为待发货，结算状态仍为未结算
        int balamt = getPaySpreadBal(compid, orderno);//订单使用的余额
        if (paytype == 4){
            result = baseDao.updateNativeSharding(compid, year, updateSQL, 0,paytype, orderno, 0);
            if (result > 0) {
                CANCEL_DELAYED.removeByKey(orderno);
                CANCEL_XXJF.add(new DelayedBase(compid, orderno));
            }
        } else {
            result = baseDao.updateNativeSharding(compid, year, updateSQL,
                    1, paytype, orderno, 0);
            if (result > 0) {
                //生成订单到一块物流
                OrderUtil.generateLccOrder(compid, orderno);
                DELIVERY_DELAYED.add(new DelayedBase(compid, orderno));
            }
        }
        if (result > 0 ) {
            //余额扣减
            if (balamt > 0) {
                IceRemoteUtil.updateCompBal(compid,-balamt);
            }
            //线下即付减数据库库存
            reduceGoodsDbStock(orderno, compid);
            return new Result().success("订单提交成功");
        }
        return new Result().fail("订单提交失败");
    }

    public static int getPaySpreadBal(int compid,String orderno){
        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TO_PAY_SQL, new Object[]{ orderno, compid, 0});
        if(list != null && list.size() > 0) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt", "odate", "otime", "pdamt", "freight", "coupamt", "distamt", "rvaddno","balamt"});
            double payamt = result[0].getPayamt();
            double bal = result[0].getBalamt();
            if(bal > 0 && payamt >= 0){
                return new Double(bal).intValue();
            }
        }
        return 0;
    }

    @UserPermission(ignore = true)
    public Result balAllPay(AppContext appContext) {
        String json = appContext.param.json;
        JSONObject jsonObject = JSON.parseObject(json);
        String orderno = jsonObject.getString("orderno");
        int compid = jsonObject.getIntValue("compid");
        if(!StringUtils.isBiggerZero(orderno) || compid <= 0){
            return new Result().fail("获取订单号或企业码失败!");
        }

        List<Object[]> list = baseDao.queryNativeSharding(compid, TimeUtils.getCurrentYear(), GET_TO_PAY_SQL,
                new Object[]{ orderno, compid, 0});

        if(!list.isEmpty()) {
            TranOrder[] result = new TranOrder[list.size()];
            BaseDAO.getBaseDAO().convToEntity(list, result, TranOrder.class, new String[]{"payamt","odate","otime","pdamt","freight","coupamt","distamt","rvaddno","balamt"});
            double bal = MathUtil.exactDiv(result[0].getBalamt(), 100)
                    .setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();


            appContext.param.arrays = new String[7];
            appContext.param.arrays[0] = orderno;
            appContext.param.arrays[1] = PAY_TYPE_YE;
            appContext.param.arrays[2] = "";
            appContext.param.arrays[3] = "1";
            appContext.param.arrays[4] = TimeUtils.date_yMd_Hms_2String(new Date());
            appContext.param.arrays[5] = String.valueOf(bal);
            appContext.param.arrays[6] = String.valueOf(compid);
            payCallBack(appContext);

            return new Result().success("支付成功");
        }else{
            return new Result().fail("未查到/已支付【"+orderno+"】的订单!");
        }
    }

    @UserPermission(ignore = true)
    public Result refund(AppContext appContext) {
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("compid").getAsInt();

        Map<String,String> r = OrderUtil.refund(compid, orderno);
        return new Result().success(r);
    }

    public static int refundBal(String orderno,int compid,int refbal,String remark){

        int ret = 0;
        List<String> sqlList = new ArrayList<>();
        List<Object[]> params = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date();
        String dateStr[] = dateFormat.format(curDate).split(" ");
        sqlList.add(INSERT_TRAN_TRANS);
        params.add(new Object[]{GenIdUtil.getUnqId(), compid, orderno, 0, refbal, 0, 0, -3, GenIdUtil.getUnqId(),
                    0,dateStr[0],dateStr[1],dateStr[0],dateStr[1],0});
        sqlList.add(INSERT_TRAN_PAYREC);//新增支付记录
        String evtInfo = "{'remark':"+remark+"}";
        params.add(new Object[]{GenIdUtil.getUnqId(),compid, 0, evtInfo, "{}", dateStr[0], dateStr[1]});
        int year = Integer.parseInt("20" + orderno.substring(0,2));
        String[] sqlNative = new String[sqlList.size()];
        sqlNative = sqlList.toArray(sqlNative);
        try{
            ret = IceRemoteUtil.updateCompBal(compid,refbal);
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }

        if(ret > 0){
            boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(compid,year, sqlNative, params));
        }
        return ret;
    }


    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }

    
    /* *
     * @description 线下即付将订单状态 未付款--->>待发货
     * @params [orderNo, compid]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/5/10 15:44
     * @version 1.1.1
     **/
    private static boolean toBeShipped(String orderNo, int compid) {
        int year = Integer.parseInt("20" + orderNo.substring(0, 2));
        String updateSQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
                + " where cstatus&1=0 and orderno=? and ostatus=? and payway=?";
        int result = baseDao.updateNativeSharding(compid, year, updateSQL, 1, orderNo, 0, 4);
        if (result > 0) {
            //生成订单到一块物流
            OrderUtil.generateLccOrder(compid, orderNo);
            CANCEL_XXJF.removeByKey(orderNo);
            DELIVERY_DELAYED.add(new DelayedBase(compid, orderNo));
        }
        return result > 0;
    }
}
