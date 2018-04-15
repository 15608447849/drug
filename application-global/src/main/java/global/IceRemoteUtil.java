package global;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;
import com.onek.entitys.Result;
import com.onek.prop.IceMasterInfoProperties;
import com.onek.util.dict.DictEntity;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProdPriceEntity;
import util.GsonUtils;
import util.StringUtils;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:08
 * 远程调用工具
 */
public class IceRemoteUtil {
    public static IceClient ic ;
    static {
        ic = new IceClient(
                IceMasterInfoProperties.INSTANCE.name,
                IceMasterInfoProperties.INSTANCE.host,
                IceMasterInfoProperties.INSTANCE.port);
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

    public static ProdPriceEntity calcSingleProdActPrize(long actcode,long sku,double vatp) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("actcode", actcode);
        jsonObject.put("sku", sku);
        jsonObject.put("vatp", vatp);
        String result = ic.settingProxy("discountServer")
                .settingReq("","DiscountCalcModule","calcSingleProdActPrize")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity.class);
    }

    public static ProdPriceEntity[] calcMultiProdActPrize(long actcode, List<ProdPriceEntity> list) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("actcode", actcode);
        JSONArray jsonArray = new JSONArray();
        if(list != null && list.size() > 0){
            for(ProdPriceEntity entity : list){
                JSONObject jsObj = new JSONObject();
                jsObj.put("sku", entity.getSku());
                jsObj.put("vatp", entity.getVatp());
                jsonArray.add(jsObj);
            }
        }
        jsonObject.put("skulist", jsonArray);
        String result = ic.settingProxy("discountServer")
                .settingReq("","DiscountCalcModule","calcMultiProdActPrize")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity[].class);
    }

    public static ProdPriceEntity calcSingleProdActIntervalPrize(long sku,double vatp) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sku", sku);
        jsonObject.put("vatp", vatp);
        String result = ic.settingProxy("discountServer")
                .settingReq("","DiscountCalcModule","calcSingleProdActIntervalPrize")
                .settingParam(jsonObject.toJSONString())
                .executeSync();
        HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
        Object data = hashMap.get("data");
        if (data == null) return null;
        String json = data.toString();
        return GsonUtils.jsonToJavaBean(json,ProdPriceEntity.class);
    }

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

}
