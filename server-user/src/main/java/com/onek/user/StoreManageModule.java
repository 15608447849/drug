package com.onek.user;

import com.onek.AppContext;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.updateStoreOp;
import util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/20 14:26
 * 门店管理
 */
public class StoreManageModule {

    /**
     * 新增门店企业信息
     */
    @UserPermission
    public Result updateStoreInfo(AppContext appContext){
        String json = appContext.param.json;
        updateStoreOp op = GsonUtils.jsonToJavaBean(json, updateStoreOp.class);
        assert op!=null;
        return op.execute(appContext);
    }




}
