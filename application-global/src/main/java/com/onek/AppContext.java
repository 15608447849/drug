package com.onek;

import Ice.Current;
import Ice.Logger;
import com.google.gson.Gson;
import com.onek.server.inf.IParam;
import com.onek.server.infimp.IApplicationContext;
import util.StringUtils;

/**
 * 平台上下文对象
 */
public class AppContext extends IApplicationContext {

    private UserSession userSession;

    public AppContext(Current current, Logger logger, IParam param) {
        super(current, logger, param);
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void setUserSession(UserSession userSession) {
        this.userSession = userSession;
    }

}
