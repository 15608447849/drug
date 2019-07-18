package com.onek.util.stock;

import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.MathUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisStockUtil {

    private static String SUCCESS = "OK";
    private static String ALL = "ALL";

    /**
     * 设置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param initStock
     * @return
     */
    public static int setStock(long sku, int initStock) {
        if(initStock > 0){
            Long result = RedisUtil.getHashProvide().putElement(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX, initStock);
            return result.intValue();
        }
        return 0;
    }

    /**
     * 设活动置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param actCode
     * @param initStock
     * @return
     */
    public static int setActStock(long sku, long actCode, int initStock, int limit) {
        HashMap<String, String> stockMap = new HashMap<>();
        stockMap.put(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + actCode, initStock+"");
        stockMap.put(RedisGlobalKeys.ACTSTOCK_PREFIX + actCode, initStock+"");
        stockMap.put(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + actCode, limit + "");
        String result = RedisUtil.getHashProvide().putHashMap(RedisGlobalKeys.STOCK_PREFIX + sku, stockMap);
        return SUCCESS.equals(result) ? 1 : 0;
    }

    /**
     * 特殊设活动置库存 1:设置成功 0:设置失败 设置ALL
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int setSpecActStock(long sku, long actCode, int limit) {
        HashMap<String, String> stockMap = new HashMap<>();
        stockMap.put(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + actCode, getStock(sku) +"");
        stockMap.put(RedisGlobalKeys.ACTSTOCK_PREFIX + actCode, ALL);
        stockMap.put(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + actCode, limit + "");
        String result = RedisUtil.getHashProvide().putHashMap(RedisGlobalKeys.STOCK_PREFIX + sku, stockMap);
        return SUCCESS.equals(result) ? 1 : 0;
    }


    /**
     * 清除活动库存 大于0代表成功; 0:代表失败
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static Long clearActStock(long sku, long actCode) {
        String init_key = RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + actCode;
        String act_key = RedisGlobalKeys.ACTSTOCK_PREFIX + actCode;
        String limit_key = RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + actCode;
        if(RedisUtil.getHashProvide().existsByKey(RedisGlobalKeys.STOCK_PREFIX + sku, init_key)){
            Long result = RedisUtil.getHashProvide().delByKeys(RedisGlobalKeys.STOCK_PREFIX + sku, init_key, act_key, limit_key);
            return result;
        }else{
            return 1L;
        }

    }

//    /**
//     * 清除初始活动库存 大于0代表成功; 0:代表失败
//     *
//     * @param sku
//     * @param actCode
//     * @return
//     */
//    public static Long clearActInitStock(long sku, long actCode) {
//        return RedisUtil.getStringProvide().delete(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode);
//    }

    public static boolean deductionActStock(long sku, int stock, List<Long> actCodes) {
        if(stock < 0){
            return false;
        }
        if(actCodes != null && actCodes.size() > 0){
            for(long actCode : actCodes){
                String key = RedisGlobalKeys.ACTSTOCK_PREFIX + actCode;
                String currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, key);
                if(ALL.equals(currentStock)){ // 活动库存为ALL,不需要判断活动库存
                    continue;
                }
                if (StringUtils.isEmpty(currentStock) && Integer.parseInt(currentStock) <= 0) {
                    return false;
                }
                if ((Integer.parseInt(currentStock) - stock) < 0) {
                    return false;
                }
            }
            for(long actCode : actCodes){
                String key = RedisGlobalKeys.ACTSTOCK_PREFIX + actCode;
                String currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, key);
                if(ALL.equals(currentStock)){ // 活动库存为ALL,不需要判断活动库存
                    continue;
                }
                Long num = RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, key, -stock);
                if (num < 0) {
                    RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, key, stock);
                    return false;
                }
            }
            RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX, -stock);
            return true;
        }
        return false;
    }

    public static int getStock(long sku) {
        String currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static int getActStockBySkuAndActno(long sku, long actCode) {
        Map<String,String> hashMap = RedisUtil.getHashProvide().getAllHash(RedisGlobalKeys.STOCK_PREFIX + sku);
        String key = RedisGlobalKeys.ACTSTOCK_PREFIX + actCode;
        String currentStock = hashMap.get(key);
        if(ALL.equals(currentStock)){ // 如果活动库存设置的是ALL取商品库存
            return getStock(sku);
        }
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        currentStock = adjustActStock(sku, hashMap, key, currentStock, actCode);
        return Integer.parseInt(currentStock);

    }

    private static String adjustActStock(long sku, Map<String, String> hashMap, String key, String currentStock, long actCode) {
        String init_key = RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + actCode;
        String initStock = hashMap.get(init_key);
        LogUtil.getDefaultLogger().info("++++++ check actcode initStock:["+initStock+"];currentStock:["+currentStock+"] +++++++");
        if(!StringUtils.isEmpty(initStock) && initStock.equals(currentStock) && !ALL.equals(currentStock)){ // 起始活动库存等于活动库存代表活动未开始
            LogUtil.getDefaultLogger().info("++++++ check actcode start +++++++");
            Map<String, Integer> stockMap = new HashMap<>();
            double sumStock = 0;
            for(String k : hashMap.keySet()){
                if(k.contains(RedisGlobalKeys.ACTSTOCK_PREFIX)){
                    String stock = hashMap.get(k);
                    if(ALL.equals(stock)){ // 活动库存为ALL,b不计算到活动库存之和
                        continue;
                    }
                    LogUtil.getDefaultLogger().info("++++++ check actcode key:["+ k+"] stock:["+ stock +"] +++++++");
                    sumStock  += Integer.parseInt(stock);
                    stockMap.put(k, Integer.parseInt(stock));
                }
            }
            int stock = hashMap.containsKey(RedisGlobalKeys.STOCK_PREFIX) ? Integer.parseInt(hashMap.get(RedisGlobalKeys.STOCK_PREFIX)) : 0 ;
            if (sumStock > 0 && sumStock > stock) {
                for (String k : stockMap.keySet()) {
                    double rate = MathUtil.exactDiv(stockMap.get(k), sumStock).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    int nas = MathUtil.exactMul(stock, rate).intValue();
                    String ac = k.replace(RedisGlobalKeys.ACTSTOCK_PREFIX, "");
                    LogUtil.getDefaultLogger().info("++++++ check actcode key:[" + k + "] ac:[" + ac + "] +++++++");
                    int limit = hashMap.containsKey(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + ac) ? Integer.parseInt(hashMap.get(RedisGlobalKeys.ACT_LIMIT_NUM_PREFIX + ac)) : 0;
                    int r = setActStock(sku, Long.parseLong(ac), nas, limit);
                    LogUtil.getDefaultLogger().info("++++++ reset actcode key:[" + k + "] result:[" + r + "] +++++++");
                    ExecutorService executors = Executors.newSingleThreadExecutor();
                    executors.execute(() -> {
                                try{
                                    IceRemoteUtil.adjustActivityStock(nas, Long.parseLong(ac), sku);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }

                    );
                }
            }
            currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, key);
        }
        return currentStock;
    }

    /**
     * 获取活动初始化库存
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int getActInitStock(long sku, long actCode) {
        String init_key = RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + actCode;
        String currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, init_key);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    /**
     * 扣减库存 0:当前库存不足没有扣减 1:扣减失败; 2:扣减成功
     *
     * @param sku
     * @param stock
     * @return
     */
    public static int deductionStock(long sku, int stock) {
        if(stock < 0){
            return 1;
        }
        String currentStock = RedisUtil.getHashProvide().getValByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        if (Integer.parseInt(currentStock) <= 0) {
            return 0;
        }
        if ((Integer.parseInt(currentStock) - stock) < 0) {
            return 0;
        }
        Long num = RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX, -stock);
        if (num < 0) {
            RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX, stock);
            return 1;
        }
        return 2;
    }

    /**
     * 添加库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long addStock(long sku, int stock) {
        Long num = RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.STOCK_PREFIX, stock);
        return num;
    }

    /**
     * 添加库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long addActStock(long sku, long actCode, int stock) {
        if(RedisUtil.getHashProvide().existsByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.ACTSTOCK_PREFIX + actCode)){
            Long num = RedisUtil.getHashProvide().incrByKey(RedisGlobalKeys.STOCK_PREFIX + sku, RedisGlobalKeys.ACTSTOCK_PREFIX + actCode, stock);
            return num;
        }
        return 0;
    }

    /**
     * 减少库存
     *
     * @param sku
     * @param stock
     * @return
     */
//    public static long deductionActStock(long sku, long actCode, int stock) {
//        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
//        if (Integer.parseInt(currentStock) <= 0) {
//            return 0;
//        }
//        if ((Integer.parseInt(currentStock) - stock) < 0) {
//            return 0;
//        }
//        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
//        calcAvailStock(sku);
//        return num;
//    }

    /**
     * 得到sku所有活动库存之和
     *
     * @param sku
     * @return
     */
    public static int getSumActStock(long sku){
        Map<String,String> map = RedisUtil.getHashProvide().getAllHash(RedisGlobalKeys.STOCK_PREFIX + sku);
        int sumStock = 0;
        if(map != null && map.size() > 0){
            for(String key : map.keySet()){
                if(key.contains(RedisGlobalKeys.ACTSTOCK_PREFIX)){
                    String stock = map.get(key);
                    if(ALL.equals(stock)){ // 活动库存为ALL,b不计算到活动库存之和里
                        continue;
                    }
                    sumStock  += Integer.parseInt(stock);
                }
            }
        }
        return sumStock;
    }


//    /**
//     * 检查活动库存设置
//     *
//     * @param skuList skuList
//     * @param calctype 计算类型 0:代表数量; 1:代表百分比
//     * @param num 基准数量/百分比值
//     * @param stockMap
//     * @return
//     */
//    public static long checkActStockSetting(List<Long> skuList, int calctype,int num, Map<Long, Integer> stockMap){
//        long start = System.currentTimeMillis();
//        Long sku = skuList.get(0);
//        String existKey = "";
//
//        Map<String,String> map = RedisUtil.getHashProvide().getAllHash(RedisGlobalKeys.AVAILSTOCK_PREFIX);
//        double rate =  MathUtil.exactDiv(num, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        if(sku == 0){ // 全部商品
//
//            if(map != null){
//                for(String key : map.keySet()){
//                    String availStock = map.get(key);
//                    if(StringUtils.isEmpty(availStock) || Integer.parseInt(availStock) <=0){
//                         existKey = key;
//                         break;
//                    }
//                    int ori_stock = stockMap.containsKey(key) ? stockMap.get(key) : 0;
//                    int new_stock = ori_stock + Integer.parseInt(availStock);
//                    int val = calctype == 0 ? num : MathUtil.exactMul(new_stock, rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue() ;
//                    if(new_stock - val < 0){
//                        existKey = key;
//                        break;
//                    }
//                }
//            }
//        }else if(sku < 100000000000L){ // 指定类别
//            for(String key : map.keySet()){
//                if(key.substring(1,key.length()-1).contains(sku+"")){
//                    String availStock = map.get(key);
//                    if(StringUtils.isEmpty(availStock) || Integer.parseInt(availStock) <=0){
//                        existKey = key;
//                    }
//                    int ori_stock = stockMap.containsKey(key) ? stockMap.get(key) : 0;
//                    int new_stock = ori_stock + Integer.parseInt(availStock);
//                    int val = calctype == 0 ? num : MathUtil.exactMul(new_stock, rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue() ;
//                    if(new_stock - val < 0){
//                        existKey = key;
//                        break;
//                    }
//                }
//
//            }
//        }else if(sku >= 100000000000L){ // 具体商品
//            for(String key : map.keySet()){
//                for(Long s : skuList){
//                    if(key.equals(s)){
//                        String availStock = map.get(key);
//                        if(StringUtils.isEmpty(availStock) || Integer.parseInt(availStock) <=0){
//                            existKey = key;
//                        }
//                        int ori_stock = stockMap.containsKey(key) ? stockMap.get(key) : 0;
//                        int new_stock = ori_stock + Integer.parseInt(availStock);
//                        int val = calctype == 0 ? num : MathUtil.exactMul(new_stock, rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue() ;
//                        if(new_stock - val < 0){
//                            existKey = key;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        long end = System.currentTimeMillis();
//        if(!StringUtils.isEmpty(existKey)){
//            return Long.parseLong(existKey);
//        }
//        System.out.println("cost "+(end - start) + " ms");
//        return 0;
//    }

    public static void main(String[] args) {
        int n = 6;
        int[] a = { Integer.MAX_VALUE,
                20, 8, 7,
                6, 4, 3 };

        int[] f = { Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };

        f[1]=a[1];
        int len=1;//通过记录f数组的有效位数，求得个数
    /*因为上文中所提到我们有可能要不断向前寻找，
    所以可以采用二分查找的策略，这便是将时间复杂
    度降成nlogn级别的关键因素。*/
        for(int i=2;i<=n;i++)
        {
            int l=0,r=len,mid;
            if(a[i]>f[len])f[++len]=a[i];
                //如果刚好大于末尾，暂时向后顺次填充
            else
            {
                while(l<r)
                {
                    mid=(l+r)/2;
                    if(f[mid]>a[i])r=mid;
                        //如果仍然小于之前所记录的最小末尾，那么不断
                        //向前寻找(因为是最长上升子序列，所以f数组必
                        //然满足单调)
                    else l=mid+1;
                }

                f[l]=Math.min(a[i],f[l]);//更新最小末尾
            }
        }

        System.out.println(len);

//        List<Long> skuList = new ArrayList<>();
//        skuList.add(0L);
//        Long key = checkActStockSetting(skuList,0,20, new HashMap<>());
//        System.out.println(key);
//        int a = RedisStockUtil.setStock(11000000011001L, 50);
//        System.out.println(a);
//        Long a= RedisStockUtil.clearActStock(11000000011001L, 18071576649925632L);
//        System.out.println(a);
//        boolean aa = RedisStockUtil.deductionActStock(11000000011001L, 5, 18071576649925632L);
//        System.out.println(aa);
//        RedisStockUtil.deductionStock(11000000011001L, 4);
//        RedisStockUtil.addStock(11000000011001L, 8);
//        RedisStockUtil.addActStock(11000000011001L, 18071576649925632L, 2);
//        int stock = RedisStockUtil.getSumActStock(11000000011001L);
//        System.out.println(stock);
//        int stock = RedisStockUtil.getActStockBySkuAndActno(11000000011001L, 18071576649925632L);
//        System.out.println(stock);
    }
}


