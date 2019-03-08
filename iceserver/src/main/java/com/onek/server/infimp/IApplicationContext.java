package com.onek.server.infimp;

import Ice.Current;
import Ice.Logger;
import com.onek.server.inf.IParam;



/**
 * @Author: leeping
 * @Date: 2019/3/8 14:32
 */
public class IApplicationContext {

    public Logger logger;
    public Current current;
    public IParam param;

    public IApplicationContext(Current current,Logger logger,IParam param){
        this.current = current;
        this.logger = logger;
        this.param = param;
    }

    /**
     * 返回this
     */
    public <T extends IApplicationContext> T convert(){
        return (T)this;
    }
}
