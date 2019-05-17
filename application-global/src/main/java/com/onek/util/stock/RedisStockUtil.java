package com.onek.util.stock;

import com.onek.util.IceRemoteUtil;
import com.onek.util.RedisGlobalKeys;
import org.hyrdpf.util.LogUtil;
import redis.util.RedisUtil;
import util.MathUtil;
import util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisStockUtil {

    private static String SUCCESS = "OK";
    private static String SEP = "|"; // 分隔符

    /**
     * 设置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param initStock
     * @return
     */
    public static int setStock(long sku, int initStock) {
        if(initStock > 0){
            String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.STOCK_PREFIX + sku, String.valueOf(initStock));
            return SUCCESS.equals(result) ? 1 : 0;
        }
        return 1;
    }

    /**
     * 设活动置库存 1:设置成功 0:设置失败
     *
     * @param sku
     * @param actCode
     * @param initStock
     * @return
     */
    public static int setActStock(long sku, long actCode, int initStock) {
        RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, String.valueOf(initStock));
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode, String.valueOf(initStock));
        return SUCCESS.equals(result) ? 1 : 0;
    }


    /**
     * 清除活动库存 大于0代表成功; 0:代表失败
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int clearActStock(long sku, long actCode) {
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, "0");
        return SUCCESS.equals(result) ? 1 : 0;
    }

    /**
     * 清除初始活动库存 大于0代表成功; 0:代表失败
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int clearActInitStock(long sku, long actCode) {
        String result = RedisUtil.getStringProvide().set(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode, "0");
        return SUCCESS.equals(result) ? 1 : 0;
    }

    public static boolean deductionActStock(long sku, int stock, long actCode) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (Integer.parseInt(currentStock) <= 0) {
            return false;
        }
        if ((Integer.parseInt(currentStock) - stock) < 0) {
            return false;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        if (num < 0) {
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
            return false;
        }
        RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        return true;
    }

    public static int getStock(long sku) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX + sku);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        return Integer.parseInt(currentStock);
    }

    public static int getActStockBySkuAndActno(long sku, long actCode) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        String initStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode);
        LogUtil.getDefaultLogger().info("++++++ check actcode initStock:["+initStock+"];currentStock:["+currentStock+"] +++++++");
        if(!StringUtils.isEmpty(initStock) && initStock.equals(currentStock)){ // 起始活动库存等于活动库存代表活动未开始
            LogUtil.getDefaultLogger().info("++++++ check actcode start +++++++");
            List<String> keys = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP);
            Map<String, Integer> stockMap = new HashMap<>();
            double sumStock = 0;
            for(String key : keys){
                String stock = RedisUtil.getStringProvide().get(key);
                LogUtil.getDefaultLogger().info("++++++ check actcode key:["+ key+"] stock:["+ stock +"] +++++++");
                sumStock  += Integer.parseInt(stock);
                stockMap.put(key, Integer.parseInt(stock));
            }
            int stock = getStock(sku);
            if (sumStock > 0 && sumStock >= stock) {
                for (String key : stockMap.keySet()) {
                    double rate = MathUtil.exactDiv(stockMap.get(key), sumStock).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                    int nas = MathUtil.exactMul(stock, rate).intValue();
                    String ac = key.split("[|]")[2];
                    LogUtil.getDefaultLogger().info("++++++ check actcode key:[" + key + "] ac:[" + ac + "] +++++++");
                    int r = setActStock(sku, Long.parseLong(ac), nas);
                    LogUtil.getDefaultLogger().info("++++++ reset actcode key:[" + key + "] result:[" + r + "] +++++++");
                    ExecutorService executors = Executors.newSingleThreadExecutor();
                    executors.execute(() -> {
                                IceRemoteUtil.adjustActivityStock(nas, Long.parseLong(ac), sku);
                            }

                    );
                }
            }
            currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        }
        return Integer.parseInt(currentStock);

    }

    /**
     * 获取活动初始化库存
     *
     * @param sku
     * @param actCode
     * @return
     */
    public static int getActInitStock(long sku, long actCode) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_INIT_PREFIX + SEP + sku + SEP + actCode);
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
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.STOCK_PREFIX + sku);
        if (StringUtils.isEmpty(currentStock)) {
            return 0;
        }
        if (Integer.parseInt(currentStock) <= 0) {
            return 0;
        }
        if ((Integer.parseInt(currentStock) - stock) < 0) {
            return 0;
        }
        Long num = RedisUtil.getStringProvide().decrease(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        if (num < 0) {
            RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
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
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
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
        RedisUtil.getStringProvide().increase(RedisGlobalKeys.STOCK_PREFIX + sku, stock);
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        return num;
    }

    /**
     * 减少库存
     *
     * @param sku
     * @param stock
     * @return
     */
    public static long deductionActStock(long sku, long actCode, int stock) {
        String currentStock = RedisUtil.getStringProvide().get(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode);
        if (Integer.parseInt(currentStock) <= 0) {
            return 0;
        }
        if ((Integer.parseInt(currentStock) - stock) < 0) {
            return 0;
        }
        Long num = RedisUtil.getStringProvide().increase(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP + actCode, stock);
        return num;
    }

    /**
     * 得到sku所有活动库存之和
     *
     * @param sku
     * @return
     */
    public static int getSumActStock(long sku){
        List<String> keys = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP);
        int sumStock = 0;
        if(keys != null && keys.size() > 0){
            for(String key : keys){
                String stock = RedisUtil.getStringProvide().get(key);
                sumStock  += Integer.parseInt(stock);
            }
        }
        return sumStock;
    }


    /**
     * 检查活动库存设置
     *
     * @param sku sku
     * @param calctype 计算类型 0:代表数量; 1:代表百分比
     * @param num 基准数量/百分比值
     * @param actcode 为0代表不需要排除关联这个活动的所有库存值; 有值代表需要排除关联这个活动的所有库存值
     * @return
     */
    public static long checkActStockSetting(long sku, int calctype,int num, long actcode){
        if(sku == 0){ // 全部商品
            List<String> keys = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.STOCK_PREFIX);
            Long key = checkStockByRedisKeys(sku, calctype, num, actcode, keys);
            if (key != null) return key;
        }else if(sku < 100000000000L){ // 指定类别
            List<String> keys1 = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.STOCK_PREFIX + 1 +sku);
            List<String> keys2 = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.STOCK_PREFIX + 2 +sku);
            keys1.addAll(keys2);
            Long key = checkStockByRedisKeys(sku, calctype, num, actcode, keys1);
            if (key != null) return key;
        }else if(sku >= 100000000000L){ // 具体商品
            List<String> keys = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.STOCK_PREFIX +sku);
            Long key = checkStockByRedisKeys(sku, calctype, num, actcode, keys);
            if (key != null) return key;
        }
        return 0;
    }

    private static Long checkStockByRedisKeys(long sku, int calctype, int num, long actcode, List<String> keys) {
        if(keys != null && keys.size() > 0){
            for(String key : keys){
                // 获取商品的库存
                String stock = RedisUtil.getStringProvide().get(key);
                if(StringUtils.isEmpty(stock)){
                    return Long.parseLong(key.replace("STOCK", ""));
                }
                List<String> _keys = RedisUtil.getStringProvide().getRedisKeyStartWith(RedisGlobalKeys.ACTSTOCK_PREFIX + SEP + sku + SEP);
                int sumStock = 0;
                if(_keys != null && _keys.size() > 0){
                    for(String _actKey : _keys){
                        String actstock = RedisUtil.getStringProvide().get(_actKey);
                        if(actcode > 0){
                            if(_actKey.contains(actcode+"")){
                                continue;
                            }
                        }
                        sumStock  += Integer.parseInt(actstock);
                    }
                }
                double rate =  MathUtil.exactDiv(num, 100F).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                int val = calctype == 0 ? num : MathUtil.exactMul(Integer.parseInt(stock), rate).setScale(0, BigDecimal.ROUND_HALF_DOWN).intValue() ;
                if((Integer.parseInt(stock) - sumStock) < val){
                    return Long.parseLong(key.replace("STOCK", ""));
                }
            }
            return 0L;
        }
        return 0L;
    }

//    public static void main(String[] args) {
//        checkActStockSetting(11000000011201L,0,20, 0);
//    }
}


