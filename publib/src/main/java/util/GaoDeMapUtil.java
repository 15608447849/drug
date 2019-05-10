package util;

import util.http.HttpRequest;

import java.net.URLEncoder;
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
        String province;
        String city;
    }


    /**
     * 获取地址经纬度 index,当前执行次数
     * 经度(longitude)在前，纬度(latitude)在后
     */
    private static String addressConvertLatLon(String address,int index){
        try {
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/geocode/geo?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("address", URLEncoder.encode(address,"UTF-8"));
            map.put("city","");
            map.put("batch","false");
            map.put("sig","");
            map.put("output","JSON");
            map.put("callback","");
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            //System.out.println(sb.toString()+"\naddress "+address+","+result);
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1 || jsonBean.geocodes.size() == 0) throw  new NullPointerException();
            return jsonBean.geocodes.get(0).location;
        } catch (Exception e) {
            index++;
            if (index<3) return addressConvertLatLon(address,index);
        }
        return null;
    }
    /**
     * 获取地址经纬度
     */
    public static String addressConvertLatLon(String address){
        return addressConvertLatLon(address.trim(),0);
    }

    /**
     * ip转地址信息
     */
    private static String ipConvertAddress(String ip,int index){
        //https://restapi.amap.com/v3/ip?ip=113.247.55.143&key=c59217680590515b7c8369ff5e8fe124
        //{"status":"1","info":"OK","infocode":"10000","province":"湖南省","city":"长沙市","adcode":"430100","rectangle":"112.6534116,27.96920845;113.3946776,28.42655248"}
        try {
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/ip?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("ip",ip);
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            if(StringUtils.isEmpty(result)) throw  new NullPointerException();
            JsonBean jsonBean = GsonUtils.jsonToJavaBean(result,JsonBean.class);
            if (jsonBean == null || jsonBean.status != 1) throw  new NullPointerException();
            return jsonBean.province+jsonBean.city;
        } catch (NullPointerException e) {
            index++;
            if (index<3) return ipConvertAddress(ip,index);
        }
        return null;
    }
    /**
     * ip转换地址信息
     */
    public static String ipConvertAddress(String ip){
        return ipConvertAddress(ip.trim(),0);
    }

}
