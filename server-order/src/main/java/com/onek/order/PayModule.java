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
        int compid = jsonObject.get("compid").getAsInt();
        String result = FileServerUtils.getPayQrImageLink("alipay","空间折叠",25.02,orderno,
                "orderServer"+getOrderServerNo(compid),"PayModule","payCallBack");
        return new Result().success(result);

    }

    public Result payCallBack(AppContext appContext){

        String [] arrays = appContext.param.arrays;
        String orderno = arrays[0];
        String paytype = arrays[1];
        String thirdPayNo = arrays[2];
        String tradeStatus = arrays[3];
        String tradeDate = arrays[4];
        String tradeTime = arrays[5];
        return new Result().success(null);
    }

    public static int getOrderServerNo(int compid){
        return compid /  GLOBALConst._DMNUM % GLOBALConst._SMALLINTMAX;
    }
}
