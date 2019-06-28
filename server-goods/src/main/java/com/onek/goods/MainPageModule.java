package com.onek.goods;

import com.onek.annotation.UserPermission;
import com.onek.entitys.Result;
import com.onek.goods.mainpagebean.Attr;
import com.onek.server.infimp.IceDebug;

/**
 * @Author: leeping
 * @Date: 2019/6/28 9:40
 * @服务名 goodsServer
 */
public class MainPageModule {
    /*
    通过活动码 获取 活动属性 及 商品信息
     */
    private static Attr dataSource(long actCode,boolean isQuery,int page,int total){
        Attr attr = new Attr();

        return attr;
    }


    /**
     * @接口摘要 客户端首页展示
     * @业务场景
     * @传参类型 json
     * @传参列表 {setup = 1-获取首页楼层  }
     * @返回列表
     */
    @IceDebug
    @UserPermission(ignore = true)
    public Result pageInfo(){

        return null;
    }




}
