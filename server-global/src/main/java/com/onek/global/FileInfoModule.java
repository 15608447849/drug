package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;

import static com.onek.util.fs.FileServerUtils.*;

/**
 * @Author: leeping
 * @Date: 2019/4/2 13:41
 */
public class FileInfoModule {

    private static class QueryParam{
        int uid;
        int compid;
        long orderid;
        long spu;
        long sku;
        ArrayList<QueryParam> list;
    }

    /**
     * 获取文件服务器
     * 上传列表/下载列表
     */
    @UserPermission (ignore = true)
    public Result fileServerInfo(AppContext appContext){
        String json = appContext.param.json;

        HashMap<String,Object> map = new HashMap<>();
            //判断前端是否存在上传文件的权限
            if (appContext.getUserSession()!=null) map.put("upUrl", fileUploadAddress());//上传url
            map.put("ergodicUrl",fileErgodicAddress());//遍历url
            map.put("downPrev",fileDownloadPrev());//下载地址url
            map.put("home",defaultHome());//资源主目录-轮播图等存放
            map.put("notice",defaultNotice());//公告目录

        if (!StringUtils.isEmpty(json)){
            QueryParam queryParam = GsonUtils.jsonToJavaBean(json,QueryParam.class);
            assert queryParam != null;
            if (queryParam.compid > 0){
                map.put("companyFilePath", companyFilePath(queryParam.compid));
                if (queryParam.orderid > 0) {
                    map.put("orderFilePath", orderFilePath(queryParam.compid,queryParam.orderid));
                }
            }
            if (queryParam.uid>0){
                map.put("userFilePath",userFilePath(queryParam.uid));
            }
            if (queryParam.spu > 0 && queryParam.sku > 0){
                map.put("goodsFilePath",goodsFilePath(queryParam.spu,queryParam.sku));
            }
            //多个spu,sku
            if (queryParam.list!=null && queryParam.list.size()>0){
                ArrayList<String> list = new ArrayList<>();
                for (QueryParam qp: queryParam.list) {
                    if (qp.spu > 0 && qp.sku > 0){
                        list.add(goodsFilePath(qp.spu,qp.sku));
                    }
                }
                map.put("goodsFilePathList",list);
            }

        }
        return new Result().success(map);
    }

}
