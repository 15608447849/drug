package com.onek.util.fs;

import com.onek.prop.FileServerProperties;
import com.onek.prop.IceMasterInfoProperties;
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
    private static FileServerProperties fsp = FileServerProperties.INSTANCE;

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




    public static String getPayQrImageLink(String type, String subject, double price,String orderNo,String serverName,String callback_clazz,String callback_method,String attr){

        List<String> list = new ArrayList<>();
            list.add(IceMasterInfoProperties.INSTANCE.name);
            list.add(IceMasterInfoProperties.INSTANCE.host);
            list.add(IceMasterInfoProperties.INSTANCE.port+"");
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

        String json = HttpUtil.formText(FileServerProperties.INSTANCE.payUrl,"POST",map);
        HashMap<String,Object> rmap = GsonUtils.string2Map(json);
        assert rmap != null;

        return rmap.get("data")==null ? null : rmap.get("data").toString();
    }

    public static void main(String[] args) {
        String res = getPayQrImageLink("alipay","控件",25.02,"15608447849010222",
                "orderserver","aplipayModule","callback",null);
        System.out.println(res);
    }
}
