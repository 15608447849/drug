package com.onek.util.stock;

import com.onek.util.RedisGlobalKeys;
import redis.util.RedisUtil;
import util.StringUtils;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RedisStockUtil {

    public static void setStock(long sku, int initStock){
        RedisUtil.getStringProvide().set(RedisGlobalKeys.STOCK_PREFIX + sku, String.valueOf(initStock));
    }

    public static void setActStock(long sku, int initStock){
        RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_PREFIX + sku, String.valueOf(initStock));
    }

    public static boolean deductionSecKillStock(long sku, int compid, int stock, int actcode){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku);
        if(Integer.parseInt(currentStock) <= 0){
            return false;
        }
        if((Integer.parseInt(currentStock) - stock) <= 0){
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actcode, stock);
        if(num < 0){
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX  + + sku + "|" +actcode, stock);
            return false;
        }
        RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        RedisUtil.getListProvide().addEndElement(RedisGlobalKeys.SECKILLPREFIX + sku, compid + "|" + stock);
        return true;
    }

    public static int getStock(long sku){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX+sku);
        if(StringUtils.isEmpty(currentStock)){
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static int getActStockBySkuAndActno(long sku, long actno){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX  + sku + "|" +actno);
        if(StringUtils.isEmpty(currentStock)){
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static boolean deductionStock(long sku, int stock){
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX+sku);
        if(StringUtils.isEmpty(currentStock)){
            return false;
        }
        if(Integer.parseInt(currentStock) <= 0){
            return false;
        }
        if((Integer.parseInt(currentStock) - stock) <= 0){
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
        if(num < 0){
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
            return false;
        }
        return true;
    }

    public static long addStock(long sku, int stock){
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX+sku, stock);
        return num;
    }

//    public static void main(String[] args) {
//        System.out.println(System.currentTimeMillis());
//        RedisStockUtil.setStock(11000000070101L, 100);
//        ExecutorService executor = Executors.newFixedThreadPool(20);
//        for (int i = 0; i < 1000; i++) {//设置1000个人来发起抢购
//            executor.execute(new MyRunnable("user"+getRandomString(6)));
//        }
//        executor.shutdown();
//    }
//
//    public static String getRandomString(int length) { //length是随机字符串长度
//        String base = "abcdefghijklmnopqrstuvwxyz0123456789";
//        Random random = new Random();
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0; i < length; i++) {
//            int number = random.nextInt(base.length());
//            sb.append(base.charAt(number));
//        }
//        return sb.toString();
//    }
//
//    static class MyRunnable implements  Runnable{
//        String userinfo;
//        public MyRunnable() {
//        }
//        public MyRunnable(String uinfo) {
//            this.userinfo=uinfo;
//        }
//        public void run() {
//            boolean flag = RedisStockUtil.deductionStock(11000000070101L, 2);
//            System.out.println("userinfo:"+userinfo+";"+flag);
//        }
//    }
}


