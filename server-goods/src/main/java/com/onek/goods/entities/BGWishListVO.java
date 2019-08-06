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


    private int cid;//企业码
    private String cname;//企业名
    private String prodname ;//药品名
    private String spec;//规格
    private String manuname;//生产厂家名称
    private int num;//数量
    private String dtaile;//备注

    private double price;//价格

    private String submitdate;//申请时间

    private String auditdate;//处理时间

    private int auditid;//处理人id
    private String auditname;//处理人
    private int cstatus;//状态

}
