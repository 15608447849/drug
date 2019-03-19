package com.onek;

import Ice.Current;
import Ice.Logger;
import IceInternal.Ex;
import com.onek.server.inf.IParam;
import com.onek.server.inf.IRequest;
import com.onek.server.infimp.IceContext;
import redis.util.RedisUtil;
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

    @Override
    protected void initialization(){
        try {
            String key = param.token + "@" + remoteIp;
            String value = RedisUtil.getStringProvide().get(key);
            if(StringUtils.isEmpty(value)) return;

            String json = RedisUtil.getStringProvide().get(value);
            if(StringUtils.isEmpty(json)) return;

            logger.print(key+" <-> Redis存在用户信息:\n" + json);
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

}
