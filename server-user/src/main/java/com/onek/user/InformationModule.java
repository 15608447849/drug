package com.onek.user;

import com.onek.AppContext;
import com.onek.FileServerUtils;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.AuditInfoOp;
import com.onek.user.operations.ChangeInfoOp;
import com.onek.user.operations.StoreBasicInfoOp;
import util.GsonUtils;

import java.util.HashMap;

import static com.onek.FileServerUtils.getCompanyPath;

/**
 * @Author: leeping
 * @Date: 2019/3/19 14:26
 *信息获取
 */
public class InformationModule {

    /**
     * 获取文件服务器上传列表/下载列表
     */
    public Result fileServerInfo(AppContext appContext){
        //获取用户信息
        return new Result().success("文件服务器信息")
                .setHashMap("upUrl", FileServerUtils.fileUploadAddress())
                .setHashMap("downPrev",FileServerUtils.fileDownloadPrev());
    }

    /**
     *  获取企业文件路径 前缀
     */
    @UserPermission
    public Result companyFilePathPrev(AppContext appContext){
        return new Result().success("企业文件路径前缀").setHashMap("companyFileDir",getCompanyPath(appContext.getUserSession().compId));
    }

    /**
     * 获取门店用户基础信息
     */
    @UserPermission
    public Result basicInfo(AppContext appContext){
        return new StoreBasicInfoOp().execute(appContext);
    }

    /**
     * 修改门店登陆手机号
     */
    @UserPermission
    public Result changeUserInfo(AppContext appContext){
        String json = appContext.param.json;
        ChangeInfoOp op = GsonUtils.jsonToJavaBean(json, ChangeInfoOp.class);
        assert op!=null;
        return op.execute(appContext);
    }

    /**
     * 修改门店登陆密码
     */

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



}
