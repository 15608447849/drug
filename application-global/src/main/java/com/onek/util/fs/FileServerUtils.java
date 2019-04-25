package com.onek.util.fs;

import com.onek.prop.AppProperties;
import util.EncryptUtils;
import util.GsonUtils;
import util.http.HttpUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/3/20 11:00
 */
public class FileServerUtils {

    private static AppProperties fsp = AppProperties.INSTANCE;

    //文件上传地址
    public static String fileUploadAddress(){
        //分布式映射动态获取前缀
        return "http://" + fsp.fileServerAddress +"/upload";
    }

    public static String fileErgodicAddress() {
        return "http://" + fsp.fileServerAddress +"/ergodic";
    }

    // 文件下载地址 前缀 ,例如 下载文件 /目录/文件.png -> 下载前缀/目录/文件.png
    public static String fileDownloadPrev(){
        return "http://" + fsp.fileServerAddress ;
    }

    public static String defaultHome(){
        return "/" + EncryptUtils.encryption(fsp.fileDefaultDir);
    }

    private static final int MOD = 500;

    //企业码->企业相关文件路径
    public static String companyFilePath(int compid) {
        int index = compid % MOD; //取模
        return "/" +index + "/" + compid + "/"+ EncryptUtils.encryption("_company") ;
    }

    //spu - 确定商品分类目录
    //spk - 确定具体商品目录
    public static String goodsFilePath(long spu,long sku){
        long index = spu % MOD;
        String path = "/" + index + "/" + spu;
        index = sku & MOD;
        path += "/" + index + "/" + sku;
        return path + "/" + EncryptUtils.encryption("_goods");
    }

    //订单号->订单相关文件路径
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


    /**
     * @return 付款二维码图片链接
     */
    public static String getPayQrImageLink(String type, String subject, double price,String orderNo,String serverName,String callback_clazz,String callback_method,String attr){

        List<String> list = new ArrayList<>();
            list.add(AppProperties.INSTANCE.masterName);
            list.add(AppProperties.INSTANCE.masterHost);
            list.add(AppProperties.INSTANCE.masterPort+"");
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

        String json = HttpUtil.formText(AppProperties.INSTANCE.payUrlPrev+"/pay","POST",map);
        HashMap<String,Object> rmap = GsonUtils.string2Map(json);
        assert rmap != null;

        return rmap.get("data")==null ? null : rmap.get("data").toString();
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

}
