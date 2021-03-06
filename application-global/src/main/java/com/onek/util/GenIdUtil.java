package com.onek.util;

import org.hyrdpf.util.LogUtil;
import redis.provide.RedisStringProvide;
import redis.util.RedisUtil;
import util.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Administrator
 * @version V1.0
 * @ClassName GenIdUtil
 * @Description TODO
 * @date 2019-03-27 19:22
 */
public class GenIdUtil {

    public static RedisStringProvide redisStringProvide = RedisUtil.getStringProvide();

    private static AtomicLong timeMills = new AtomicLong(0);

    private final static SnowflakeIdWorker idWorker = new SnowflakeIdWorker(0,0);

    /**
     * 通过redis生成全局唯一ID
     * @param key redis对应的key
     * @param isExpire 是否设置过期（默认是晚上零点失效）
     * @return
     */
    private static Long generateId(String key,boolean isExpire) {
        long autoId = 0;
        try {
            if(isExpire){
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                redisStringProvide.pexpire(key,calendar.getTimeInMillis());
            }
            autoId = redisStringProvide.increase(key,1);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.getDefaultLogger().debug("redis生成全局唯一ID失败");
        }
        return autoId;
    }


    /**
     *
     * @param id redis生成的唯一ID
     * @param prefix 生成ID的前缀
     * @param date 日期
     * @param minLength 最小长度
     * @return
     */
    private static String format(Long id, String prefix, Date date,
                                 Integer minLength,int compid,int type){
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        if(date != null){
            DateFormat df = new SimpleDateFormat("yyMMdd");
            sb.append(df.format(date));
        }
        String strId =  String.valueOf(id);
        int length = strId.length();
        if(length < minLength){
            for(int i = 0;i < minLength - length; i++){
                sb.append("0");
            }
            sb.append(strId);
        }else{
            sb.append(strId);
        }
        if(type == 0){
            sb.append(compid / GLOBALConst._DMNUM  % GLOBALConst._SMALLINTMAX);
            sb.append(compid % GLOBALConst._DMNUM  % GLOBALConst._MODNUM_EIGHT);
        }

        return sb.toString();
    }


    /**
     * 年（2位）+月（2位）+日（2位）+ 自增位（8位）+ 服务器坐标（3位）+ 数据库坐标（1位）
     * @return
     */
    public static String getOrderId(int compid){
        long autoId = generateId("order",true);
        return format(autoId,"",new Date(),8,compid,0);
    }


    /**
     * 年（2位）+月（2位）+日（2位）+ 自增位（6位）
     * @return
     */
    public static String getAsOrderId(){
        long autoId = generateId("asorder",true);
        return format(autoId,"",new Date(),6,0,1);
    }


    /**
     * 获取企业编码（自增长+数据库编号+表编号）
     * @return
     */
    public static int getCompId(){
        int compId = 0;
        try {
            compId = redisStringProvide.increase("comp",1).intValue()+GLOBALConst.COMP_INIT_VAR;
        } catch (Exception e) {
            LogUtil.getDefaultLogger().info("redis生成企业编码失败");
            e.printStackTrace();
        }
        return compId;
    }


    /**
     * 生成唯一码
      */
    public static long getUnqId(){
        String srvidtmp = System.getProperty("group");
        String subSrvidtmp = System.getProperty("index");
        int srvid = 0;
        int subSrvid = 0;
        if(!StringUtils.isEmpty(srvidtmp) && srvidtmp.contains("-")){
            srvidtmp = srvidtmp.substring(srvidtmp.lastIndexOf("-")+1);
            if(StringUtils.isInteger(srvidtmp)){
                srvid = Integer.parseInt(srvidtmp);
            }
        }
        if(!StringUtils.isEmpty(subSrvidtmp)){
            subSrvid = Integer.parseInt(subSrvidtmp);
        }
        idWorker.setDatacenterId(srvid);
        idWorker.setWorkerId(subSrvid);
        return idWorker.nextId();
    }


    public static String getDbServerByOrderno(String orderno){

        if(StringUtils.isEmpty(orderno)){
            return null;
        }
        String dbserver = "";
        switch (orderno.length()) {
            case 16:
                dbserver = orderno.substring(orderno.length() - 2);
                break;
            case 17:
                dbserver = orderno.substring(orderno.length() - 3);
                break;
            default:
                dbserver = orderno.substring(orderno.length() - 4);
        }
        return dbserver;
    }
}

