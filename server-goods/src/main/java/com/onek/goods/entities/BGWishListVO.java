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

    /**运营后台查询心愿单详情*/
    public static String _QUERY_WISHLIST = "SELECT" +
            "wl.cid cid,"+
            "comp.cname cname," +
            "wl.prodname prodname," +
            "wl.manuname manuname," +
            "wl.spec spec," +
            "wl.num num," +
            "wl.dtaile dtaile," +
            "wl.price/100 price," +
            "CONCAT( wl.submitdate, ' ', wl.submittime ) submitdate," +
            "CONCAT( wl.auditdate, ' ', wl.audittime ) auditdate," +
            "su.urealname auditname ," +
            "wl.cstatus cstatus, " +
            "wl.auditid auditid" +
            "FROM" +
            "{{?" + DSMConst.TD_WISH_LIST + "}} wl" +
            "LEFT JOIN {{? " + DSMConst.TB_COMP + " }} comp ON wl.cid = comp.cid" +
            "LEFT JOIN {{? " + DSMConst.TB_SYSTEM_USER + " }} su ON wl.auditid = su.uid";

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
