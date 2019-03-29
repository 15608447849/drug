package com.onek.user;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.operations.AuditInfoOp;

import com.onek.user.operations.UpdateAuditOp;
import util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/3/27 13:06
 */
public class BackgroundAuditModule {
    /**
     * 管理后台查询
     * 查询字段 : 1.申请账号 2.企业名 3.提交审核时间 4.审核时间
     */
    public Result queryAuditInfo(AppContext appContext){
        String json = appContext.param.json;
        AuditInfoOp op = GsonUtils.jsonToJavaBean(json, AuditInfoOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 管理后台审核
     */
    public Result updateAudit(AppContext appContext){
        String json = appContext.param.json;
        UpdateAuditOp op =  GsonUtils.jsonToJavaBean(json, UpdateAuditOp.class);
        assert op!=null;
        return op.execute(appContext);
    }
}
