package com.onek.util.fs;

import com.onek.prop.FileServerProperties;
import util.EncryptUtils;

import javax.swing.text.html.parser.Entity;
import java.math.BigInteger;

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

   /* public static void main(String[] args) {
        System.out.println("可配置的默认目录: "+ defaultHome());
        System.out.println("企业相关文件目录: "+ companyFilePath(536862726));
        System.out.println("具体商品相关目录: "+ goodsFilePath(110101099910L,11010109991001L));
        System.out.println("订单售后相关目录: "+ orderFilePath(536862726,190325000000010011L));
    }*/

}
