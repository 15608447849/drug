package com.onek.goods.mainpagebean;

import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.onek.goods.entities.ProdVO;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/6/28 9:42
 */
public class Attr {
    //活动码
    public long actCode;
    //活动对象
    public Object actObj;
    //商品列表
    public List<ProdVO> list;
    //分页信息
    public PageHolder page;

}
