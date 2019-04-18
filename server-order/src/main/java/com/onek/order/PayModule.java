package com.onek.order;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.fs.FileServerUtils;
import global.GLOBALConst;

public class PayModule {

    @UserPermission(ignore = true)
    public Result prePay(AppContext appContext){

        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String orderno = jsonObject.get("orderno").getAsString();
        int compid = jsonObject.get("orderno").getAsInt();
        String result = FileServerUtils.getPayQrImageLink("alipay","空间折叠",25.02,orderno,
                "orderServer"+getOrderServerNo(compid),"PayModule","payCallBack");
        return new Result().success(result);

    }

    public Result payCallBack(AppContext appContext){

        return new Result().success(null);
    }

    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }
}
