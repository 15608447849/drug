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
            {"d_global_dict"}, // 用户表 1
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
            {"d_syn_log"}, //17
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




    // TODO　切分表的规则数组 ：0 代表不需要切分，1 代表按公司模型切分表,2 标识是否需要主从同步
    // 索引必须同_DB_TABLES里的索引对应
    public final static int[] SEG_TABLE_RULE = {
            0, 0, 0, 0, 3,
            1, 0, 0, 0, 0,
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
