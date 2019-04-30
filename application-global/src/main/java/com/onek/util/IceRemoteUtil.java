package com.onek.util;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.util.area.AreaEntity;
import com.onek.util.dict.DictEntity;
import com.onek.util.member.MemberEntity;
import com.onek.util.prod.ProdEntity;
import util.GsonUtils;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:08
 * 远程调用工具
 */
public class IceRemoteUtil {
    private final static IceClient ic = new IceClient(
            AppProperties.INSTANCE.masterName,
            AppProperties.INSTANCE.masterHost,
            AppProperties.INSTANCE.masterPort)
            .startCommunication();

    /**
     * 根据企业码 获取 分库分表的订单服务的下标序列
     */
    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }

    //获取商品名
    public static String getProduceName(String pclass) {
        try {
           return ic.setServerAndRequest("globalServer","CommonModule","getProduceName").setArrayParams(pclass).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //地区码->地区详细名
    public static String getCompleteName(String areaCode) {
        try {
            return ic.setServerAndRequest("globalServer","CommonModule","getCompleteName").setArrayParams(areaCode).execute().replace("\"","").trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

    }

    public static ProdEntity getProdBySku(long sku) {
        try {
            String result = ic.setServerAndRequest("globalServer","CommonModule","getProdBySku").setArrayParams(sku).execute();
            return GsonUtils.jsonToJavaBean(result,ProdEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * lzp
     * 0短信模板序列id ,1及以后:模板需要的占位符信息参数
     */
    public static String getMessageByNo(String... args){
        try {
            return ic.setServerAndRequest("globalServer","MessageModule","convertMessage")
                    .settingParam(args)
                    .execute().replace("\"","").trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getMessageByNo(int tempNo,String... params){
        try {
            String[] args = new String[params.length+1];
            args[0] = tempNo+"";
            System.arraycopy(params, 0, args, 1, params.length);
            return getMessageByNo(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DictEntity getId(Object id) {
        try {
            String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","getId")
                    .setArrayParams(id)
                    .execute();
            return GsonUtils.jsonToJavaBean(result,DictEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getArean(long areac) {
        String result = ic.setServerAndRequest("globalServer","CommonModule","getAreaName")
                .setArrayParams(areac)
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        return data == null ? "" : data.toString();
    }

    public static AreaEntity getAreaByAreac(long areac){
        String result = ic.setServerAndRequest("globalServer","CommonModule","getArea")
                .setArrayParams(areac)
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,AreaEntity.class);
    }

    public static AreaEntity[] getAncestors(long areac){
        String result = ic.setServerAndRequest("globalServer","CommonModule","getAncestors")
                .setArrayParams(areac)
                .execute();

        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,AreaEntity[].class);
    }


    public static DictEntity[] queryAll() {
        String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","queryAll").execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }

    public static DictEntity[] queryByParams(String [] params) {
        String result = ic.setServerAndRequest("globalServer","DictUtilRemoteModule","queryByParams").settingParam(params).execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        assert hashMap != null;
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }


    public static int collectCoupons(int compid,String content){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","insertRevCoupon")
                .settingParam(content)
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    /**
     * 发送消息到指定客户端
     * 消息规则 :  push:消息模板ID#消息模板参数1#消息模板参数2#...
     */
    public static void sendMessageToClient(int compid,String message){
        int index = getOrderServerNo(compid);
        ic.settingProxy("orderServer"+index).sendMessageToClient(compid+"",message);
    }

    public static void sendTempMessageToClient(int compid,int tempNo,String... params){
        sendMessageToClient(compid,SmsTempNo.genPushMessageBySystemTemp(tempNo,params));
    }

    //发送消息到全部客户端
    public static void sendMessageToAllClient(String message){
        List<String> list = getAllCompId();
        for (String compid : list)
            try {
                sendMessageToClient(Integer.parseInt(compid), message);
            } catch (NumberFormatException e) {
            }
    }

    //发送消息到全部客户端
    public static void sendMessageToAllClient(int tempNo,String... params){
        List<String> list = getAllCompId();
        for (String compid : list)
            try {
                sendMessageToClient(Integer.parseInt(compid), SmsTempNo.genPushMessageBySystemTemp(tempNo,params));
            } catch (NumberFormatException ignored) {
            }
    }

    //获取全部的公司码ID
    public static List<String> getAllCompId() {
        String json = ic.setServerAndRequest("userServer","StoreManageModule","getAllCompId").execute();
        return GsonUtils.json2List(json,String.class);
    }

    //获取全部门店用户手机号码
    public static List<String> getAllStorePhone() {
        String json = ic.setServerAndRequest("userServer","StoreManageModule","getAllUserPhone").execute();
        return GsonUtils.json2List(json,String.class);
    }

    //查询所有足迹
    public static List<String> queryFootprint(int compid){
        int index = getOrderServerNo(compid);

        String json = ic.setServerAndRequest("orderServer"+index,"MyFootprintModule","backQuery")
                .setArrayParams(compid).execute();
        return GsonUtils.json2List(json,String.class);
    }

    //添加积分明细
    public static int addIntegralDetail(int compid,int istatus,int integral,long busid){

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("istatus", istatus);
        jsonObject.put("integral", integral);
        jsonObject.put("busid", busid);
        int index = getOrderServerNo(compid);

        String json = ic.setServerAndRequest("orderServer"+index,"IntegralModule","addIntegral")
                .settingParam(jsonObject.toJSONString()).execute();
        Result ret = GsonUtils.jsonToJavaBean(json,Result.class);
        assert ret != null;
        return ret.code;
    }


    public static int addPoint(int compid, int point){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("point", point);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","addPoint")
                .settingParam(jsonObject.toJSONString())
                .execute();
        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
        assert ret != null;
        return ret.code;
    }

    public static MemberEntity getMemberByCompid(int compid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","getMember")
                .settingParam(jsonObject.toJSONString())
                .execute();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,MemberEntity.class);
    }

    /**
     * lzp
     * 获取企业信息 json
     */
    public static String getCompanyJson(int compid) {
        try {
            return ic.setServerAndRequest("userServer","LoginRegistrationModule","getStoreInfo").setArrayParams(compid).execute();
        } catch (Exception ignored) {
        }
        return null;
    }

    public static long getOrderCntByCompid(int compid){
        String result = ic.setServerAndRequest("orderServer"+getOrderServerNo(compid),"CouponRevModule","getOrderCntByCompid")
                .setArrayParams(compid)
                .execute();
        return Long.parseLong(result);

    }



}


//
//    public static HashMap<Object, Object> getEffectiveRule() {
//        String result = ic.setServerAndRequest("discountServer","DiscountCalcModule","getEffectiveRule")
//                .execute();
//        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
//        assert hashMap != null;
//        Object data = hashMap.get("data");
//        if (data == null) return null;
//        String json = data.toString();
//        return GsonUtils.string2Map(json);
//    }
//    public static int reducePoint(int compid, int point){
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("compid", compid);
//        jsonObject.put("point", point);
//        String result = ic.settingProxy("userServer")
//                .settingReq("","MemberModule","reducePoint")
//                .settingParam(jsonObject.toJSONString())
//                .execute();
//        Result ret = GsonUtils.jsonToJavaBean(result,Result.class);
//        assert ret != null;
//        return ret.code;
//    }

//    public static ProdPriceEntity calcSingleProdActPrize(long actcode,long sku,double vatp) {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("actcode", actcode);
//        jsonObject.put("sku", sku);
//        jsonObject.put("vatp", vatp);
//        String result = ic.settingProxy("discountServer")
//                .settingReq("","DiscountCalcModule","calcSingleProdActPrize")
//                .settingParam(jsonObject.toJSONString())
//                .execute();
//        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
//        Object data = hashMap.get("data");
//        if (data == null) return null;
//        String json = data.toString();
//        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity.class);
//    }

//    public static ProdPriceEntity[] calcMultiProdActPrize(long actcode, List<ProdPriceEntity> list) {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("actcode", actcode);
//        JSONArray jsonArray = new JSONArray();
//        if(list != null && list.size() > 0){
//            for(ProdPriceEntity entity : list){
//                JSONObject jsObj = new JSONObject();
//                jsObj.put("sku", entity.getSku());
//                jsObj.put("vatp", entity.getVatp());
//                jsonArray.add(jsObj);
//            }
//        }
//        jsonObject.put("skulist", jsonArray);
//        String result = ic.settingProxy("discountServer")
//                .settingReq("","DiscountCalcModule","calcMultiProdActPrize")
//                .settingParam(jsonObject.toJSONString())
//                .execute();
//        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
//        Object data = hashMap.get("data");
//        if (data == null) return null;
//        String json = data.toString();
//        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity[].class);
//    }
//
//    public static ProdPriceEntity calcSingleProdActIntervalPrize(long sku,double vatp) {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("sku", sku);
//        jsonObject.put("vatp", vatp);
//        String result = ic.settingProxy("discountServer")
//                .settingReq("","DiscountCalcModule","calcSingleProdActIntervalPrize")
//                .settingParam(jsonObject.toJSONString())
//                .execute();
//        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
//        Object data = hashMap.get("data");
//        if (data == null) return null;
//        String json = data.toString();
//        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity.class);
//    }



