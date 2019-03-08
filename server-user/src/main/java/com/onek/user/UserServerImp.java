package com.onek.user;

import Ice.Current;
import Ice.Logger;
import com.onek.AppContext;
import com.onek.server.inf.IParam;
import com.onek.entitys.Result;
import com.onek.server.infimp.IApplicationContext;

public class UserServerImp {

    public Result login(AppContext appContext){

       return new Result().success("登陆成功");
    }

}
