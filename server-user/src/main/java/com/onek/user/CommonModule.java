package com.onek.user;

import com.alibaba.fastjson.JSONObject;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.area.AreaStore;
import com.onek.util.dict.DictStore;

public class CommonModule {

    public Result getAreas(AppContext appContext) {
        return new Result().success(AreaStore.getTreeJson());
    }

    public Result getDicts(AppContext appContext){
        JSONObject j = DictStore.getAllDict();
        return new Result().success(j.toJSONString());
    }
}
