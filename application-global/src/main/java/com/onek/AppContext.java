package com.onek;

import Ice.Current;
import Ice.Logger;
import IceInternal.Ex;
import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IceContext;
import redis.util.RedisUtil;
import util.EncryptUtils;
import util.GsonUtils;
import util.StringUtils;

/**
 * 平台上下文对象
 */
public class AppContext extends IceContext {

    /**
     * session有效时间 以秒为单位
     */
    public static final int SESSION_EFFECTIVE_SESSIONS = 60;

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
            if(StringUtils.isEmpty(value)) return;

            String json = RedisUtil.getStringProvide().get(value);
            if(StringUtils.isEmpty(json)) return;
            //logger.print(key+" - Redis存在用户信息:\n" + json);
            this.userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);

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



}
