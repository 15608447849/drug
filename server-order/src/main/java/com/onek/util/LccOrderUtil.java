package com.onek.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.hsf.framework.api.cstruct.UserParam;
import com.hsf.framework.api.myOrder.MyOrderServerPrx;
import com.hsf.framework.order.OrderICE;
import com.hsf.framework.order.OrderServicePrx;
import com.onek.entity.TranOrder;
import com.onek.property.LccProperties;
import util.MathUtil;
import util.StringUtils;

import java.util.Set;

public class LccOrderUtil {

    public static String addLccOrder(TranOrder tranOrder,String arriarc) {

        OrderICE orderIce = new OrderICE();

        orderIce.pubercompid = LccProperties.INSTANCE.pubercompid + "";
        orderIce.puberid = LccProperties.INSTANCE.puberid + "";

        orderIce.phone1 = "";
        orderIce.phone2 = "";
        orderIce.billno = tranOrder.getOrderno();

        orderIce.startc = LccProperties.INSTANCE.startc + "";
        orderIce.startaddr = LccProperties.INSTANCE.startaddr;
        orderIce.arriarc = arriarc;
        orderIce.arriaddr = tranOrder.getAddress();

        orderIce.wm = 0.1;
        orderIce.wmdictc = "43"; // 立方
        orderIce.num = tranOrder.getPdnum();
        orderIce.numdictc = "51"; //件
        orderIce.padictc = "61";
        orderIce.ctdictc = "32"; // 药品

        orderIce.vnum = 1;
        orderIce.vldictc = "1";
        orderIce.vtdictc = "11";
        orderIce.tndictarr = new int[] { 71 };
        double freight = MathUtil.exactDiv(tranOrder.getFreight(), 100).doubleValue();
        orderIce.price = freight;
        orderIce.codamt = 0;

        orderIce.pmdictc = "81"; // 整单
        orderIce.insureamt = 0;

        orderIce.consignee = tranOrder.getConsignee();
        orderIce.consphone = tranOrder.getContact();
        orderIce.ptdictc = "22"; // 线下到付
        orderIce.dmdictc = "93"; // 送货不上楼
        orderIce.redictc = "101"; // 无回单
        orderIce.priority = 1; // 定向发布
        orderIce.source = 3;

        OrderServicePrx orderServicePrx = (OrderServicePrx) RpcClientUtil
                .getServicePrx(OrderServicePrx.class);
        orderIce.orderno = orderServicePrx.generateOrderNo();

        UserParam param = new UserParam();
        param.uid = orderIce.puberid;
        param.compid = orderIce.pubercompid;

        String result = orderServicePrx.addOrder(param, orderIce);
        System.out.println(result);

//        UserParam robbParam = new UserParam();
//        robbParam.uid = LccProperties.INSTANCE.robbid + "";
//        robbParam.compid = LccProperties.INSTANCE.robbcompid + "";
//        result = orderServicePrx.robbingOrder(robbParam, new String[] {orderIce.orderno, orderIce.pubercompid});
//
//        System.out.println(result);

        return result;

    }

    public static JSONObject queryTraceByOrderno(String orderno){
        MyOrderServerPrx myOrderServerPrx = (MyOrderServerPrx)RpcClientUtil.getServicePrx(MyOrderServerPrx.class);
        UserParam userParam = new UserParam();
        userParam.compid = LccProperties.INSTANCE.pubercompid + "";
        String result = myOrderServerPrx.getOrderTraByOrderid(userParam, new String[]{orderno}, 0);
        JSONObject traceJson = new JSONObject();
        System.out.println(result);
        if(!StringUtils.isEmpty(result)){
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(Integer.parseInt(jsonObject.get("code").toString()) == 0){
                 JSONObject data = (JSONObject) jsonObject.get("obj");
                 if(data == null){
                     traceJson.put("billno", "");
                     traceJson.put("logictype", "0");
                     traceJson.put("node", new JSONArray());
                     return traceJson;
                 }
                 Set<String> keys = data.keySet();
                 if(keys != null && keys.size() > 0){
                     String key = keys.iterator().next();
                     traceJson.put("billno", key);
                     traceJson.put("logictype", "0");
                     JSONArray array = data.getJSONArray(key);
                     JSONArray node = new JSONArray();
                     for(Object obj : array){
                         if(obj instanceof String){
                             if(obj.toString().charAt(0) == '{'){
                                 JSONObject correct = JSONObject.parseObject(obj.toString());
                                 if(correct.containsKey("correct")){
                                     continue;
                                 }
                             }else if(obj.toString().charAt(0) == '['){
                                 node = JSONObject.parseArray(obj.toString());
                                 break;
                             }
                         }

                     }
                     traceJson.put("node", node);
                 }
            }
        }

        return traceJson;
    }

//    static {
//        AppConfig.initLogger("log4j2.xml");
//        AppConfig.initialize();
//    }
//
//    public static void main(String[] args) {
//        JSONObject result = LccOrderUtil.queryTraceByOrderno("1904220003127703");
//        System.out.println(result.toJSONString());
//    }
}
