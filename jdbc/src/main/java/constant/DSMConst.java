package constant;

import org.hyrdpf.ds.UDAConst;

/**
 * Copyright © 2018空间折叠【FOLDING SPACE】. All rights reserved.
 * @ClassName: DSMConst
 * @Description: TODO 业务数据库中相关数据库对象元数常量类，包含表，列等信息
 * @version: V1.0
 */
public interface DSMConst extends UDAConst {



    // TODO　相关数据库 表对象 常量，与数据库一一对应。位置一旦固定，不要轻易修改，因为代码里都是用其位置来获得数据的。
    public final static String[][] DB_TABLES = {
            //索引0对应表名，从索引1开始对应这个表的列名，表名与列名必须同数据库一致！
            {"tb_system_user"}, // 用户表  0
            {"tb_global_dict"}, // 字典 1
            {"tb_system_role"}, // 角色表 2
            {"tb_system_resource"}, // 资源表 3
            {"tb_order_base"}, // 4
            {"tb_order_product"},  // 5
            {"tb_sms_template"},//短信模板 6
            {"tb_comp_ship_info"},//收货人 7
            {"tb_comp"},//企业表 8
            {"tb_global_area"}, // 地区 9
            {"tb_comp_invoice"}, // 发票 10
            {"tb_comp_aptitude"}, //企业资质表 11
            {"td_produce_class"}, //产品类别表 12
            {"td_prod_brand"}, //药品品牌表 13
            {"td_prod_manu"}, //生产厂商表 14
            {"td_prod_sku"}, //产品SKU表 15
            {"td_prod_spu"}, //产品SPU表 16
            {"td_syn_log"}, //17 日志表
            {"td_prom_act"}, //18 活动表
            {"td_prom_rule"}, //19 活动规则表
            {"td_prom_ladoff"}, //20 规则阶梯优惠表
            {"td_prom_assgift"}, //21 优惠赠换商品表
            {"td_prom_gift"}, //22 赠换商品表
            {"td_prom_coupon"}, //23 优惠券表
            {"td_prom_assdrug"}, //24 活动商品关联表
            {"td_prom_time"}, //25 活动场次表
            {"td_prom_group"}, //26 组团情况表
            {"td_prom_couent"}, //27 优惠券领取表
            {"td_member_level"}, //28 会员等级表
            {"td_tran_colle"}, //29 我的收藏表
            {"tb_mall_floor"}, //30 楼层表
            {"td_footprint"},//31 我的足迹表
            {"td_push_msg"}, //32 推送消息表
            {"tb_area_pca"}, //33 省市区县表
            {"tb_area_street"}, //34 街表
            {"tb_area_villages"}, //35 村表
            {"td_prom_courcd"}, //36 优惠券领取记录表
            {"td_prom_rela"}, //37 优惠券领取记录表
            {"td_member"}, //38 会员
            {"td_signin"}, //39签到
            {"td_integral_detail"}, //40 
            {"td_tran_order"}, //41订单表
            {"td_tran_goods"}, //42 订单商品表
            {"td_tran_trans"}, //43 订单交易表
            {"td_tran_appraise"}, //44 订单评价表
            {"td_tran_payrec"}, //45 支付记录表
            {"td_bk_tran_order"}, //46 后台订单表
            {"td_bk_tran_goods"}, //47 后台订单商品表
            {"td_tran_asapp"}, //48 后台售后表
            {"td_notice"}, //49 公告信息表
            {"td_prom_offlcoup"}, //50 线下优惠券表
            {"tb_proxy_notice"}, //51 区域广播表
            {"tb_proxy_noticearec"}, //52 区域广播地区表
            {"tb_proxy_noticedt"}, //53 区域广播接收表
            {"tb_proxy_uarea"} //54 渠道区域表
    };


    //TODO 表名=表在_DB_TABLES常量里固定位置值 ，目的是为了开发人员写代码方便可读性
    //TODO 系统-用户表 0
    public final static int TB_SYSTEM_USER = 0;
    public final static int TB_GLOBAL_DICT = 1;
    public final static int TB_SYSTEM_ROLE = 2;
    public final static int TB_SYSTEM_RESOURCE = 3;
    public final static int TB_ORDER_BASE = 4;
    public final static int TB_ORDER_PRODUCT = 5;
    public final static int TB_SMS_TEMPLATE = 6;
    public final static int TB_COMP_SHIP_INFO = 7;
    public final static int TB_COMP = 8;
    public final static int TB_GLOBAL_AREA = 9;
    public final static int TB_COMP_INVOICE = 10;
    public final static int TB_COMP_APTITUDE = 11;
    public final static int TD_PRODUCE_CLASS = 12;
    public final static int TD_PROD_BRAND = 13;
    public final static int TD_PROD_MANU = 14;
    public final static int TD_PROD_SKU = 15;
    public final static int TD_PROD_SPU = 16;
    public final static int TD_SYN_LOG = 17;
    public final static int TD_PROM_ACT = 18;
    public final static int TD_PROM_RULE = 19;
    public final static int TD_PROM_LADOFF = 20;
    public final static int TD_PROM_ASSGIFT = 21;
    public final static int TD_PROM_GIFT = 22;
    public final static int TD_PROM_COUPON = 23;
    public final static int TD_PROM_ASSDRUG = 24;
    public final static int TD_PROM_TIME = 25;
    public final static int TD_PROM_GROUP = 26;
    public final static int TD_PROM_COUENT = 27;
    public final static int TD_MEMBER_LEVEL = 28;
    public final static int TD_TRAN_COLLE = 29;
    public final static int TB_MALL_FLOOR = 30;
    public final static int TD_FOOTPRINT = 31;
    public final static int TD_PUSH_MSG = 32;

    public final static int TB_AREA_PCA = 33;
    public final static int TB_AREA_STREET = 34;
    public final static int TB_AREA_VILLAGES = 35;
    public final static int TD_PROM_COURCD = 36;
    public final static int TD_PROM_RELA = 37;
    public final static int TD_MEMBER = 38;
    public final static int TD_SIGNIN = 39;
    public final static int TD_INTEGRAL_DETAIL = 40;
    public final static int TD_TRAN_ORDER = 41;
    public final static int TD_TRAN_GOODS = 42;
    public final static int TD_TRAN_TRANS = 43;
    public final static int TD_TRAN_APPRAISE = 44;
    public final static int TD_TRAN_PAYREC = 45;
    public final static int TD_BK_TRAN_ORDER = 46;
    public final static int TD_BK_TRAN_GOODS = 47;
    public final static int TD_TRAN_ASAPP = 48;
    public final static int TD_NOTICE = 49;
    public final static int TD_PROM_OFFLCOUP = 50;
    public final static int TD_PROXY_NOTICE = 51;
    public final static int TD_PROXY_NOTICEAREC = 52;
    public final static int TD_PROXY_NOTICEDT = 53;
    public final static int TD_PROXY_UAREA = 54;

    // TODO　切分表的规则数组 ：0 代表不需要切分（默认主从同步），1 代表按公司模型切分表,2 无需要主从同步
    // 索引必须同_DB_TABLES里的索引对应
    public final static int[] SEG_TABLE_RULE = {
            2, 0, 0, 0, 3,// 0-4
            1, 0, 0, 2, 0,//5-9
            0, 2, 0, 0, 0,//10-14
            0, 0, 0, 0, 0,//15-19
            0, 0, 0, 0, 0,//20-24
            0, 0, 1, 0, 3,//25-29
            0, 3, 3, 0, 0,//30-34
            0, 0, 0, 0, 1,//35-39
            1, 1, 1, 1, 1,//40-44
            1, 4, 4, 4, 2,//45-49
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0};



    //索引0对应表名，从索引1开始对应这个表的列名，表名与列名必须同数据库一致！
    public final static String[] TABLES_UNIQUE_KEY = {
            "uid","0", "roleid","resourceid", "orderno", "0",
            "0", "shipid", "cid"};


}
