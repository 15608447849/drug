package com.onek.goods.entities;

import com.google.gson.JsonObject;
import com.onek.entitys.Result;
import constant.DSMConst;

import java.math.BigDecimal;

/**
 * @author  lz
 * 心愿单运营后台逻辑处理
 */
public class BGWishListVO {

    public int oid ;//唯一码
    public int cid;//企业码
    public String cname;//企业名
    public String prodname ;//药品名
    public String manuname;//生产厂家名称
    public String spec;//规格
    public int num;//数量
    public String dtaile;//备注
    public double price;//价格
    public String submitdate;//申请时间
    public String auditdate;//处理时间
    public String auditname;//处理人
    public int cstatus;//状态
    public int auditid;//处理人id
    public String useType;//操作状态

}
