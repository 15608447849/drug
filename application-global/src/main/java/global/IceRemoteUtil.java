package global;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;
import com.onek.entitys.Result;
import com.onek.prop.AppProperties;
import com.onek.util.dict.DictEntity;
import com.onek.util.member.MemberEntity;
import com.onek.util.prod.ProdEntity;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:08
 * 远程调用工具
 */
public class IceRemoteUtil {
    public static IceClient ic ;
    static {
        ic = new IceClient(
                AppProperties.INSTANCE.masterName,
                AppProperties.INSTANCE.masterHost,
                AppProperties.INSTANCE.masterPort);
      ic.startCommunication();
    }

    public static String getProduceName(String pclass) {
        try {

            String result = ic.settingProxy("globalServer")
                    .settingReq("","CommonModule","getProduceName")
                    .settingParam(new String[]{pclass})
                    .executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            Object data = hashMap.get("data");
            if (data!=null) {
                return data.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCompleteName(String areaCode) {
        try {

            String result = ic.settingProxy("globalServer")
                    .settingReq("","CommonModule","getCompleteName")
                    .settingParam(new String[]{areaCode})
                    .executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            Object data = hashMap.get("data");
            if (data!=null) {
                return data.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ProdEntity getProdBySku(long sku) {
        try {
            String result = ic.settingProxy("globalServer")
                    .settingReq("","CommonModule","getProdBySku")
                    .settingParam(new String[]{sku+""})
                    .executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            Object data = hashMap.get("data");
            if (data==null) return null;
            String json = data.toString();
            ProdEntity prodEntity = GsonUtils.jsonToJavaBean(json,ProdEntity.class);
            return prodEntity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param args 0短信模板序列id ,1及以后:模板需要的占位符信息参数
     */
    public static String getMessageByNo(String... args){
        try {
            String result = ic.settingProxy("globalServer")
                    .settingReq("","MessageModule","convertMessage")
                    .settingParam(args)
                    .executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            Object data = hashMap.get("data");
            if (data==null) return null;
            String message = data.toString();
           if (!StringUtils.isEmpty(message)) return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static DictEntity getId(Object id) {
        String result = ic.settingProxy("globalServer")
                .settingReq("","DictUtilRemoteModule","getId")
                .settingParam(new String[]{id+""})
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity.class);
    }

    public static String getArean(long areac) {
        String result = ic.settingProxy("globalServer")
                .settingReq("","CommonModule","getAreaName")
                .settingParam(new String[]{String.valueOf(areac)})
                .executeSync();

        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());

        Object data = hashMap.get("data");

        return data == null ? "" : data.toString();
    }

    public static DictEntity[] queryAll() {
        String result = ic.settingProxy("globalServer")
                .settingReq("","DictUtilRemoteModule","queryAll")
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }

    public static DictEntity[] queryByParams(String [] params) {
        String result = ic.settingProxy("globalServer")
                .settingReq("","DictUtilRemoteModule","queryByParams")
                .settingParam(params)
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,DictEntity[].class);
    }

//    public static ProdPriceEntity calcSingleProdActPrize(long actcode,long sku,double vatp) {
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("actcode", actcode);
//        jsonObject.put("sku", sku);
//        jsonObject.put("vatp", vatp);
//        String result = ic.settingProxy("discountServer")
//                .settingReq("","DiscountCalcModule","calcSingleProdActPrize")
//                .settingParam(jsonObject.toJSONString())
//                .executeSync();
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
//                .executeSync();
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
//                .executeSync();
//        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
//        Object data = hashMap.get("data");
//        if (data == null) return null;
//        String json = data.toString();
//        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity.class);
//    }

    public static int collectCoupons(int compid,String content){
        String result = ic.settingProxy("orderServer"+getOrderServerNo(compid))
                .settingReq("","CouponRevModule","insertRevCoupon")
                .settingParam(content)
                .executeSync();
        Result ret = GsonUtils.jsonToJavaBean(result,new TypeToken<Result>(){}.getType());
        return ret.code;
    }

    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }

    /**
     * 发送消息到指定客户端
     * 消息规则 :  push:消息模板ID#消息模板参数1#消息模板参数2#...
     */
    public static void sendMessageToClient(int compid,String message){
        int index = getOrderServerNo(compid);
        ic.settingProxy("orderServer"+index).sendMessageToClient(compid+"",message);
    }

    //查询所有足迹
    public static ArrayList<LinkedTreeMap> queryFootprint(int compid){
        int index = getOrderServerNo(compid);
        HashMap<String,Object> hashMap = new HashMap<>();
        hashMap.put("compid",compid);
        String result = ic.settingProxy("orderServer"+index).settingReq("","MyFootprintModule","query")
                .settingParam(GsonUtils.javaBeanToJson(hashMap)).executeSync();
        hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        return (ArrayList<LinkedTreeMap>)data;
    }

    public static HashMap<Object, Object> getEffectiveRule() {
        JSONObject jsonObject = new JSONObject();
        String result = ic.settingProxy("discountServer")
                .settingReq("","DiscountCalcModule","getEffectiveRule")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.string2Map(json);
    }

    public static int addPoint(int compid, int point){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("point", point);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","addPoint")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        Result ret = GsonUtils.jsonToJavaBean(result,new TypeToken<Result>(){}.getType());
        return ret.code;
    }

    public static int reducePoint(int compid, int point){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        jsonObject.put("point", point);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","reducePoint")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        Result ret = GsonUtils.jsonToJavaBean(result,new TypeToken<Result>(){}.getType());
        return ret.code;
    }

    public static MemberEntity getMemberByCompid(int compid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("compid", compid);
        String result = ic.settingProxy("userServer")
                .settingReq("","MemberModule","getMember")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,MemberEntity.class);
    }

    /**
     * 获取企业信息 json
     * @return
     */
    public static String getCompanyJson(int compid) {
        try {
            String result = ic.settingProxy("userServer").settingParam(new String[]{compid+""}).executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            return hashMap.get("data").toString();
        } catch (Exception ignored) {
        }
        return null;
    }
}
