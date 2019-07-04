package com.onek.util.fs;

import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.onek.prop.AppProperties;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;
import util.http.HttpRequest;
import util.http.HttpUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/3/20 11:00
 */
public class FileServerUtils {

    private static AppProperties fsp = AppProperties.INSTANCE;

    //文件上传地址
    public static String fileUploadAddress(){
        //分布式映射动态获取前缀
        return fsp.fileServerAddress +"/upload";
    }

    public static String fileErgodicAddress() {
        return fsp.fileServerAddress +"/ergodic";
    }

    public static String fileDeleteAddress() {
        return fsp.fileServerAddress +"/delete";
    }

    // 文件下载地址 前缀 ,例如 下载文件 /目录/文件.png -> 下载前缀/目录/文件.png
    public static String fileDownloadPrev(){
        return fsp.fileServerAddress ;
    }

    //excel
    public static String getExcelDre(){
        return "/" + EncryptUtils.encryption("_excel");
    }

    //首页信息主目录
    public static String defaultHome(){
        return "/" + EncryptUtils.encryption("_home");
    }

    //公告目录
    public static String defaultNotice(){
        return  "/" + EncryptUtils.encryption("_notice");
    }

    //图片验证码路径
    public static String defaultVerificationDir() {
        return   "/" + EncryptUtils.encryption("image_verification_code");
    }

    private static final int MOD = 500;

    //企业码->企业相关文件路径
    public static String companyFilePath(int compid) {
        int index = compid % MOD; //取模
        return "/" +index + "/" + compid + "/"+ EncryptUtils.encryption("_company") ;
    }

    //用户码->用户相关文件路径
    public static String userFilePath(int uid) {
        int index = uid % MOD; //取模
        return "/" +index + "/" + uid + "/"+ EncryptUtils.encryption("_user") ;
    }



    //spu - 确定商品分类目录 ,spk - 确定具体商品目录
    public static String goodsFilePath(long spu,long sku){
        long index = spu % MOD;
        String path = "/" + index + "/" + spu;
        index = sku & MOD;
        path += "/" + index + "/" + sku;
        return path + "/" + EncryptUtils.encryption("_goods");
    }

    //订单相关文件路径
    public static String orderFilePath(int compid,long orderid){
        int index = compid % MOD; //取模
        String path = "/" +index + "/" + compid + "/" +EncryptUtils.encryption("_order_all");
        String arr = String.valueOf(orderid);
        String y = arr.substring(0,2);
        String m = arr.substring(2,4);
        String d = arr.substring(4,6);
        path += "/" + y +"/" + m + "/" + d;
        long index2 = orderid % MOD;
        path += "/" + index2 + "/" +orderid;
        return path;
    }

    //检查一个具体文件是否存在
    public static boolean isFileExist(String filePath) {
        String json = new HttpRequest().getTargetDirFileList(fileErgodicAddress(),filePath,false).getRespondContent();
        if (StringUtils.isEmpty(json)) return false;
        HashMap<String,Object> map = GsonUtils.string2Map(json);
        if (map!=null){
            Object data = map.get("data");
            try { return Boolean.parseBoolean(data.toString()); } catch (Exception ignored) { }
        }
        return false;
    }

    //检查目录存在哪些文件
    public static List<String> showDirFilesPath(String dirPath,boolean isSubDir) {
        String json = new HttpRequest().getTargetDirFileList(fileErgodicAddress(),dirPath,isSubDir).getRespondContent();
        if (StringUtils.isEmpty(json)) return null;
        HashMap<String,Object> map = GsonUtils.string2Map(json);
        if (map!=null){
            Object data = map.get("data");
            if (data instanceof List) return (List<String>) data;
        }
        return null;
    }


    private static HashMap<String,Object> accessPayServer(String type, String subject, double price,String orderNo,String serverName,String callback_clazz,String callback_method,String attr,boolean isApp){
        List<String> list = new ArrayList<>();
        String[] arr = AppProperties.INSTANCE.iceServers.split(";");
        String[] iceAddr = arr[0].split(":");

        list.add(AppProperties.INSTANCE.iceInstance);
        list.add(AppProperties.INSTANCE.iceServers);
        list.add(serverName);
        list.add(callback_clazz);
        list.add(callback_method);
        if (attr!=null && attr.length()>0)  list.add(attr);

        String body = String.join("@",list);
        HashMap<String,String> map = new HashMap<>();
        map.put("type",type);
        map.put("subject",subject);
        map.put("price",String.valueOf(price));
        map.put("orderNo",orderNo);
        map.put("body",body);
        map.put("app",String.valueOf(isApp));
        String json = HttpUtil.formText(AppProperties.INSTANCE.payUrlPrev+"/pay","POST",map);
        return GsonUtils.string2Map(json);
    }

    /**
     * @return 付款二维码图片链接
     */
    public static String getPayQrImageLink(String type, String subject, double price,String orderNo,String serverName,String callback_clazz,String callback_method,String attr) {
        HashMap<String, Object> rmap = accessPayServer(type, subject, price, orderNo, serverName, callback_clazz, callback_method, attr, false);
        assert rmap != null;
        return rmap.get("data") == null ? null : rmap.get("data").toString();
    }

    public static Map getAppPayInfo(String type, String subject, double price, String orderNo, String serverName, String callback_clazz, String callback_method, String attr){
        HashMap<String, Object> rmap = accessPayServer(type, subject, price, orderNo, serverName, callback_clazz, callback_method, attr, true);
        assert rmap != null;
        return rmap.get("data") == null ? null : (Map)rmap.get("data");
    }
    /**
     * 查询一个订单支付状态
     * @return 0-待支付 1已支付 -2异常
     */
    public static String getPayCurrentState(String type,String orderNo){
        HashMap<String,String> map = new HashMap<>();
        map.put("type",type);
        map.put("orderNo",orderNo);
        String json = HttpUtil.formText(AppProperties.INSTANCE.payUrlPrev+"/query","POST",map);
        HashMap<String,Object> rmap = GsonUtils.string2Map(json);
        assert rmap != null;
        return rmap.get("data")==null ? null : rmap.get("data").toString();
    }

    /**
     * 退款
     * @return 退款申请信息
     */
    public static HashMap<String,Object> refund(String type, String refundNo,String tradeNo,double price){
        HashMap<String,String> map = new HashMap<>();
        map.put("type",type);
        map.put("refundNo",refundNo);
        map.put("price",String.valueOf(price));
        map.put("tradeNo",tradeNo);
        String json = HttpUtil.formText(AppProperties.INSTANCE.payUrlPrev+"/refund","POST",map);
        return GsonUtils.string2Map(json);
    }

    /**
     *获取excel下载路径
     */
    public static String getExcelDownPath(String fileName, InputStream excelStream){
        try {
            //上传文件
            String json = new HttpRequest().addStream(
                    excelStream,
                    FileServerUtils.getExcelDre(),  //远程路径
                    EncryptUtils.encryption(fileName)+".xls")//文件名
                    .fileUploadUrl(FileServerUtils.fileUploadAddress())//文件上传URL
                    .getRespondContent();
            HashMap<String,Object> maps = GsonUtils.jsonToJavaBean(json,new TypeToken<HashMap<String,Object>>(){}.getType());
            ArrayList<LinkedTreeMap<String,Object>> list = (ArrayList<LinkedTreeMap<String, Object>>) maps.get("data");
            assert list != null;
            return FileServerUtils.fileDownloadPrev()+list.get(0).get("relativePath").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
