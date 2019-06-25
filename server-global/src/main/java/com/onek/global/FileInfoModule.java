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
 * @服务名 globalServer
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
     * 获取
     * 上传列表/下载列表
     */
    /**
     * @接口摘要 文件服务器/路径信息
     * @业务场景 用于文件的上传下载 / 统一目录
     * @传参类型 JSON - 可选项, 获取相关路径需要组合使用
     * @传参列表
     * {uid-用户码,compid-公司码,orderid-订单号,spu,sku 商品类别/区分的编码,List-{sku,spu} 多个商品标识}
     * @返回列表
     * 必带:{upUrl-文件上传服务器的地址,ergodicUrl文件遍历地址,downPrev文件下载地址,home系统资源及轮播图存放路径,notice公告资源存放路径},
     * 组合产生:{companyFilePath企业相关路径,feedbackPath企业意见反馈路径,orderFilePath订单售后先关路径,userFilePath用户资源相关路径,goodsFilePath商品相关路径}
     */
    @UserPermission (ignore = true)
    public Result fileServerInfo(AppContext appContext){
        String json = appContext.param.json;

        HashMap<String,Object> map = new HashMap<>();
            map.put("upUrl", fileUploadAddress());//上传url
            map.put("ergodicUrl",fileErgodicAddress());//遍历url
            map.put("downPrev",fileDownloadPrev());//下载地址url
            map.put("home",defaultHome());//资源主目录-轮播图等存放
            map.put("notice",defaultNotice());//公告目录


        if (!StringUtils.isEmpty(json)){
            QueryParam queryParam = GsonUtils.jsonToJavaBean(json,QueryParam.class);
            assert queryParam != null;
            if (queryParam.compid > 0){
                map.put("companyFilePath", companyFilePath(queryParam.compid));//公司路径
                map.put("feedbackPath", companyFilePath(queryParam.compid)+"/feedback");//意见反馈路径
                if (queryParam.orderid > 0) {
                    map.put("orderFilePath", orderFilePath(queryParam.compid,queryParam.orderid));//售后订单相关路径
                }
            }
            if (queryParam.uid>0){
                map.put("userFilePath",userFilePath(queryParam.uid));//用户相关路径
            }
            if (queryParam.spu > 0 && queryParam.sku > 0){
                map.put("goodsFilePath",goodsFilePath(queryParam.spu,queryParam.sku));//商品相关路径
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
