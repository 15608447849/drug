package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.util.sqltransfer.SqlRemoteReq;
import com.onek.util.sqltransfer.SqlRemoteResp;
import dao.BaseDAO;
import util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/5/20 10:23
 */
public class InternalCallModule {

    @UserPermission(ignore = true)
    public SqlRemoteResp updateBatchNative(AppContext context){
        SqlRemoteResp resp = new SqlRemoteResp();
        try {
            String json = context.param.json;
            SqlRemoteReq req = GsonUtils.jsonToJavaBean(json,SqlRemoteReq.class);
            context.logger.print("执行:"+ req);
            assert req != null;
            resp.resArr = BaseDAO.getBaseDAO().updateBatchNative(req.sql,req.params,req.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

    @UserPermission(ignore = true)
    public SqlRemoteResp queryNative(AppContext context){
        SqlRemoteResp resp = new SqlRemoteResp();
        try {
            String json = context.param.json;
            SqlRemoteReq req = GsonUtils.jsonToJavaBean(json,SqlRemoteReq.class);
            context.logger.print("执行:"+ req);
            assert req != null;
            resp.lines = BaseDAO.getBaseDAO().queryNative(req.sql,req.objects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

    @UserPermission(ignore = true)
    public SqlRemoteResp updateNative(AppContext context){
        SqlRemoteResp resp = new SqlRemoteResp();
        try {
            String json = context.param.json;
            SqlRemoteReq req = GsonUtils.jsonToJavaBean(json,SqlRemoteReq.class);
            context.logger.print("执行:"+ req);
            assert req != null;
            resp.res = BaseDAO.getBaseDAO().updateNative(req.sql,req.objects);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

    @UserPermission(ignore = true)
    public SqlRemoteResp updateTransNative(AppContext context){
        SqlRemoteResp resp = new SqlRemoteResp();
        try {
            String json = context.param.json;
            SqlRemoteReq req = GsonUtils.jsonToJavaBean(json,SqlRemoteReq.class);
            context.logger.print("执行:"+ req);
            assert req != null;
            resp.resArr = BaseDAO.getBaseDAO().updateTransNative(req.sqlArr,req.params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
    }

}
