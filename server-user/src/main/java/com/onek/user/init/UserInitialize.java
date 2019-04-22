package com.onek.user.init;

import com.onek.context.StoreBasicInfo;
import com.onek.server.infimp.IIceInitialize;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.GsonUtils;

import java.math.BigDecimal;
import java.util.List;

import static Ice.Application.communicator;
import static util.StringUtils.checkObjectNull;

public class UserInitialize implements IIceInitialize {
    @Override
    public void startUp(String serverName) {
        //启动线程更新企业信息
        new Thread(()->{
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            while (true){
                try {
                    updateCompInfoToCache();
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(15 * 60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateCompInfoToCache() {
        final String selectSql = "SELECT cstatus,examine,cname,caddr,caddrcode,lat,lng,cid FROM {{?"+ DSMConst.D_COMP+"}} WHERE ctype=0";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql);
        if (lines == null) return;
        for (Object[] rows : lines){
            StoreBasicInfo info = new StoreBasicInfo();

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
            info.latitude = checkObjectNull(rows[5],new BigDecimal(0)); //纬度
            info.longitude = checkObjectNull(rows[6],new BigDecimal(0)); //精度
            info.storeId = checkObjectNull(rows[7],0); //公司码
            String json = GsonUtils.javaBeanToJson(info);
            String res = RedisUtil.getStringProvide().set(""+info.storeId,json );
            communicator().getLogger().print("更新 - Redis - 公司信息:\n" + json+" - "+res);
        }

    }
}
