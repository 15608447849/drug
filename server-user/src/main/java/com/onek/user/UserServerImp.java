package com.onek.user;

import Ice.Current;
import Ice.Logger;
import com.onek.server.inf.IParam;
import com.onek.entitys.Result;

public class UserServerImp {

    public Result login(Current __current,Logger logger,IParam params){
       logger.print("调用登陆方法");
       return new Result().success("登陆成功");
    }

}
