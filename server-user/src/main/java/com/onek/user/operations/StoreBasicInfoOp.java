package com.onek.user.operations;

import com.onek.context.StoreBasicInfo;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.GsonUtils;

import java.math.BigDecimal;
import java.util.List;

import static com.onek.util.IceRemoteUtil.getCompleteName;
import static util.StringUtils.checkObjectNull;

/**
 * @Author: leeping
 * @Date: 2019/3/20 16:30
 * 企业信息
 */
public class StoreBasicInfoOp {
    //更新企业信息到缓存
    public static void updateCompInfoToCache() {
        final String selectSql = "SELECT cstatus,examine,cname,caddr,caddrcode,lat,lng,cid FROM {{?"+ DSMConst.TB_COMP +"}} WHERE ctype=0";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
        if (lines == null) return;
        for (Object[] rows : lines){
            StoreBasicInfo info = new StoreBasicInfo(checkObjectNull(rows[7],0));
            objArrToStoreInfo(rows,info);
            infoToCache(info);
        }
    }
    //更具企业码更新企业信息到缓存
    public static void updateCompInfoToCacheById(int cid){
        StoreBasicInfo info = new StoreBasicInfo(cid);
        getStoreInfoById(info);
       infoToCache(info);
    }

    //企业信息json保存到缓存 -> 企业码 = 企业信息json
    public static void infoToCache(StoreBasicInfo info) {
        if (info.storeId>0){
            RedisUtil.getStringProvide().set(""+info.storeId, GsonUtils.javaBeanToJson(info) );
        }
    }
    //获取企业信息
    public static boolean getStoreInfoById(StoreBasicInfo info) {
        //通过企业码获取企业信息
        String selectSql = "SELECT cstatus,examine,cname,caddr,caddrcode,lat,lng" +
                " FROM {{?"+ DSMConst.TB_COMP +"}}"+
                " WHERE ctype=0 AND cid = ?";
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
        if ((status&64) == 64){
            status = 64; //待认证
            info.authenticationMessage = "待认证";
        }else if ( (status&128) == 128 ){
            status = 128; //审核中
            info.authenticationMessage = "审核中";
        }else if ((status&256) == 256){
            status = 256; //已认证
            info.authenticationMessage = "已认证";
        }else if ((status&512) == 512){
            status = 512; //认证失败
            info.authenticationMessage = "认证失败 "+checkObjectNull(rows[1],"");
        }else if ((status&1024) == 1024){
            status = 1024; //停用
            info.authenticationMessage = "已停用";
        }
        info.authenticationStatus = status;
        info.storeName = checkObjectNull(rows[2],"");
        info.address = checkObjectNull(rows[3],"未设置");
        info.addressCode = checkObjectNull(rows[4],0L);
        info.addressCodeStr = getCompleteName(checkObjectNull(rows[4],""));
        info.latitude = new BigDecimal(checkObjectNull(rows[5],"0.00")); //纬度
        info.longitude = new BigDecimal(checkObjectNull(rows[6],"0.00")); //精度

    }


}
