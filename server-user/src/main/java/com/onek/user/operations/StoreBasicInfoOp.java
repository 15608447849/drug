package com.onek.user.operations;

import com.onek.context.StoreBasicInfo;
import com.onek.util.IceRemoteUtil;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.GsonUtils;

import java.math.BigDecimal;
import java.util.List;

import static constant.DSMConst.TB_COMP;
import static util.StringUtils.checkObjectNull;

/**
 * @Author: leeping
 * @Date: 2019/3/20 16:30
 * 企业信息
 */
public class StoreBasicInfoOp {
    //系统启动时,更新企业信息到缓存
    public static void updateCompInfoToCache() {
        final String selectSql = "SELECT cid FROM {{?"+ TB_COMP +"}} WHERE ctype=0";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
        if (lines == null) return;
        for (Object[] rows : lines){
            updateCompInfoToCacheById(checkObjectNull(rows[0],0),false);
        }
    }

    //企业码更新企业信息到缓存
    public static StoreBasicInfo updateCompInfoToCacheById(int cid,boolean isRefClientInfo){
       StoreBasicInfo info = new StoreBasicInfo(cid);
       getStoreInfoById(info);
       infoToCache(info);
        //通知客户端刷新
       if (isRefClientInfo) IceRemoteUtil.sendMessageToClient(cid,"ref:刷新用户信息");
       return info;
    }

    //企业信息 json保存到缓存 -> 企业码 = 企业信息json
    private static void infoToCache(StoreBasicInfo info) {
        if (info.storeId>0){
            String key = info.storeId + "";
            RedisUtil.getStringProvide().delete(key);
            RedisUtil.getStringProvide().set(key, GsonUtils.javaBeanToJson(info) );
            RedisUtil.getStringProvide().expire(key,24 * 60 * 60 ); //存活一天
        }
    }

    //获取企业信息
    public static boolean getStoreInfoById(StoreBasicInfo info) {
        //通过企业码获取企业信息
        String selectSql = "SELECT cstatus,examine,cname,caddr,caddrcode,lat,lng,storetype,control " +
                " FROM {{?"+ TB_COMP +"}}"+
                " WHERE ctype=0 AND cid=?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql, info.storeId);
        if (lines.size() == 1){
            //获取企业信息
            Object[] rows = lines.get(0);
            objArrToStoreInfo(rows,info);
            return true;
        }
      return false;
    }
    //设置认证信息等
    private static void objArrToStoreInfo(Object[] rows,StoreBasicInfo info){
        int status = (int) rows[0];
       if ( (status&128) == 128 ){
            status = 128; //审核中
            info.authenticationMessage = "审核中";
        }else if ((status&256) == 256){
            status = 256; //已认证
            info.authenticationMessage = "认证成功";
        }else if ((status&512) == 512){
            status = 512; //认证失败
            info.authenticationMessage = "认证失败 "+checkObjectNull(rows[1],"");
        }else if ((status&1024) == 1024){
            status = 1024; //停用
            info.authenticationMessage = "停用使用";
        }
        info.authenticationStatus = status;
        info.storeName = checkObjectNull(rows[2],"");
        info.address = checkObjectNull(rows[3],"");
        info.addressCode = checkObjectNull(rows[4],0L);
        info.latitude = new BigDecimal(checkObjectNull(rows[5],"0.00")); //纬度
        info.longitude = new BigDecimal(checkObjectNull(rows[6],"0.00")); //精度
        info.storetype = checkObjectNull(rows[7],0);
        info.controlCode = checkObjectNull(rows[8],0);

    }


}
