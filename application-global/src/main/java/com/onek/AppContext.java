package com.onek;

import Ice.Current;
import Ice.Logger;
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

    public AppContext(Current current, IRequest request) {
        super(current, request);
    }

    @Override
    protected void initialization() {
        String token = param.token;
        if(!StringUtils.isEmpty(token)){
            String json = RedisUtil.getStringProvide().get(token);
            if(!StringUtils.isEmpty(json)){
                this.userSession = GsonUtils.jsonToJavaBean(json, UserSession.class);
            }
        }
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

}
