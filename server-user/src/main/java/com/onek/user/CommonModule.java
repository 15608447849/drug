package com.onek.user;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.area.AreaStore;

public class CommonModule {
    public Result getAreas(AppContext appContext) {
        return new Result().success(AreaStore.getTreeJson());
    }
}
