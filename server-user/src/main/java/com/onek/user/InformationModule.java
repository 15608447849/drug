package com.onek.user;

import com.onek.AppContext;
import com.onek.FileServerUtils;
import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.user.operations.StoreBasicInfoOp;

/**
 * @Author: leeping
 * @Date: 2019/3/19 14:26
 * 门店使用
 * 信息获取
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
     * 获取门店用户基础信息
     */
    @UserPermission
    public Result basicInfo(AppContext appContext){
        return new StoreBasicInfoOp().execute(appContext);
    }




}
