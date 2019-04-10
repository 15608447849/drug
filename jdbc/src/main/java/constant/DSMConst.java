package constant;

import org.hyrdpf.ds.AppConfig;
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
            {"d_system_user"}, // 用户表  0
            {"d_global_dict"}, // 字典 1
            {"d_system_role"}, // 角色表 2
            {"d_system_resource"}, // 资源表 3
            {"d_order_base"}, // 4
            {"d_order_product"},  // 5
            {"d_sms_template"},//短信模板 6
            {"d_comp_ship_info"},//收货人 7
            {"d_comp"},//企业表 8
            {"d_global_area"}, // 地区 9
            {"d_comp_invoice"}, // 发票 10
            {"d_comp_aptitude"}, //企业资质表 11
            {"d_produce_class"}, //产品类别表 12
            {"td_prod_brand"}, //药品品牌表 13
            {"td_prod_manu"}, //生产厂商表 14
            {"td_prod_sku"}, //产品SKU表 15
            {"td_prod_spu"}, //产品SPU表 16
            {"d_syn_log"}, //17 日志表
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
            {"td_push_msg"}//32 推送消息表

    };


    //TODO 表名=表在_DB_TABLES常量里固定位置值 ，目的是为了开发人员写代码方便可读性
    //TODO 系统-用户表 0
    public final static int D_SYSTEM_USER = 0;
    public final static int D_GLOBAL_DICT = 1;
    public final static int D_SYSTEM_ROLE = 2;
    public final static int D_SYSTEM_RESOURCE = 3;
    public final static int D_ORDER_BASE = 4;
    public final static int D_ORDER_PRODUCT = 5;
    public final static int D_SMS_TEMPLATE = 6;
    public final static int D_COMP_SHIP_INFO = 7;
    public final static int D_COMP = 8;
    public final static int D_GLOBAL_AREA = 9;
    public final static int D_COMP_INVOICE = 10;
    public final static int D_COMP_APTITUDE = 11;
    public final static int D_PRODUCE_CLASS = 12;
    public final static int TD_PROD_BRAND = 13;
    public final static int TD_PROD_MANU = 14;
    public final static int TD_PROD_SKU = 15;
    public final static int TD_PROD_SPU = 16;
    public final static int D_SYN_LOG = 17;
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



    // TODO　切分表的规则数组 ：0 代表不需要切分（默认主从同步），1 代表按公司模型切分表,2 无需要主从同步
    // 索引必须同_DB_TABLES里的索引对应
    public final static int[] SEG_TABLE_RULE = {
            0, 0, 0, 0, 3,// 0-4
            1, 0, 0, 0, 0,//5-9
            0, 0, 0, 0, 0,//10-14
            0, 0, 0, 0, 0,//15-19
            0, 0, 0, 0, 0,//20-24
            0, 0, 0, 0, 3,//25-29
            0, 3, 3, 0, 0,//30-34
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
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
