package global;

import util.GsonUtils;
import util.StringUtils;
import util.http.HttpRequest;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/12 15:29
 */
public class GaoDeMapUtil {
    public static String apiKey = "c59217680590515b7c8369ff5e8fe124";

    private static class Geocode{
        String location;
    }

    private static class JsonBean{
        int status;
        int count;
        List<Geocode> geocodes;
    }

    /**
     * 获取地址经纬度
     * 经度(longitude)在前，纬度(latitude)在后
     */
    public static String addressConvertLatLon(String address){
        StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/geocode/geo?");
        HashMap<String,String> map = new HashMap<>();
        map.put("key",apiKey);
        map.put("address",address);
        map.put("city","");
        map.put("batch","false");
        map.put("sig","");
        map.put("output","JSON");
        map.put("callback","");
        String result = new HttpRequest().bindParam(sb,map).getRespondContent();
        if(StringUtils.isEmpty(result)) return null;
        JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
        if (jsonBean == null || jsonBean.status != 1) return null;
        return jsonBean.geocodes.get(0).location;
    }

}
