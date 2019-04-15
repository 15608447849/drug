package com.onek.context;

import Ice.Current;
import com.onek.entitys.Result;
import com.onek.server.inf.IRequest;
import com.onek.server.inf.PushMessageClientPrx;
import com.onek.server.infimp.IceContext;
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
        try {

            String key = param.token + "@" + remoteIp;
            String value = RedisUtil.getStringProvide().get(key);

//            logger.print(key+" = " + value);

            if(StringUtils.isEmpty(value)) return;

            String json = RedisUtil.getStringProvide().get(value);
//            logger.print(value+" = " + json);

            if(StringUtils.isEmpty(json)) return;

            this.userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);
//            logger.print(key+" - Redis存在用户信息:\n" + userSession);

        } catch (Exception ignored) { }
    }

    public UserSession getUserSession() {
        return userSession;
    }
        //登录时设置
    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }


    //创建用户会话KRY lzp
    private String createKey() {
        param.token = param.token + "@" + remoteIp;
        return EncryptUtils.encryption(param.token);
    }

    //创建用户会话到缓存 lzp
    public boolean relationTokenUserSession() {
        if (userSession == null) return false;
        //创建token标识
        String key = createKey();
        String json = GsonUtils.javaBeanToJson(userSession);
        logger.print("缓存用户信息:\n"+ json);
        String res = RedisUtil.getStringProvide().set(key,json );
        if (res.equals("OK")){
            res = RedisUtil.getStringProvide().set(param.token , key);
            return res.equals("OK");
        }
        return false;
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
