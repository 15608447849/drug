package com.onek.context;

import Ice.Current;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.PushMessageClientPrx;
import com.onek.server.infimp.IceContext;
import global.IceRemoteUtil;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;

import java.util.Map;

/**
 * 平台上下文对象
 */
public class AppContext extends IceContext {


    private UserSession userSession;

    public AppContext(Current current, IRequest request)  {
        super(current, request);
    }

    //初始化用户信息 lzp
    @Override
    protected void initialization(){

            //加载用户信息
            if (StringUtils.isEmpty(param.token)) return;
            String key = genUKey();
            String json = RedisUtil.getStringProvide().get(key);
            if(StringUtils.isEmpty(json)) return;
            logger.print( key + " #------- 当 前 用 户 信 息 ----->> " +json );
            this.userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);

            //查询企业信息
        if (userSession.compId > 0) {
            json = RedisUtil.getStringProvide().get(userSession.compId+"");
            if(StringUtils.isEmpty(json)) {
                //远程调用查询
                json = IceRemoteUtil.getCompanyJson(userSession.compId);
                if(StringUtils.isEmpty(json))  return;
            };
            logger.print( key + " #------- 当 前 用 户 企 业 信 息 ----->> " +json );
            userSession.comp = GsonUtils.jsonToJavaBean(json, StoreBasicInfo.class);
        }
    }

    public UserSession getUserSession() {
        return userSession;
    }
        //登录时设置
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }


    //创建用户会话KRY lzp
    private String genUKey() {
        return EncryptUtils.encryption(param.token + "@" + remoteIp);
    }

    //创建用户会话到缓存 lzp
    public boolean relationTokenUserSession() {
        if (userSession == null) return false;
        //创建token 标识
        String key = genUKey();
        String json = GsonUtils.javaBeanToJson(userSession);

        String res = RedisUtil.getStringProvide().set(key,json);
        logger.print( key + " ||===写入用户信息到缓存======== "+ res +" =======>> "+ json);
        return  res.equals("OK");
    }

    @Override
    protected void longConnectionSetting(Map<String, PushMessageClientPrx> map, Result result) {
        if (userSession==null || userSession.compId == 0) return;
        String key = userSession.compId+"";
        PushMessageClientPrx clientPrx = map.get(key);
        if (clientPrx == null) {
            result.setRequestOnline();
        } else {
            try {
                clientPrx.ice_ping();
            } catch (Exception e) {
                result.setRequestOnline();
            }
        }
    }

}
