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
            {"lc_global_dict"}, // 公共字典表
            {"lc_global_area"}, // 区域表
            {"lc_user_user"}, // 用户信息表
            {"lc_user_role"}, // 用户角色表
            {"lc_user_sms"}, // 短信验证表
            {"lc_user_token"}, //用户TOKEN表
            {"lc_user_comp"},//承运商基础表
            {"lc_user_comp_circle"}, // 承运商圈子表
            {"lc_user_res"}, // 承运商司机表
            {"lc_user_sms_template"}, //承运商司机关系表
            {"lc_carrier_message"},//承运商消息表
            {"lc_carrier_passage_transfer"}, // 承运商路线途经/中转表
            {"lc_user_comp_route"}, // 承运商路线表
            {"lc_user_order_count"},//承运商订单统计表
            {"lc_order_base"}, // 订单表 lc_order_base
            {"lc_order_trac"}, // 订单扩展表 lc_order_trac
            {"lc_order_trajectory"}, // 行程单纪录表
            {"lc_order_trans"}, // 订单运输表
            {"lc_order_evaluate"}, // 订单评价表
            {"lc_order_redundancy"}, //订单冗余表
            {"lc_user_comp_credentimg"}, //企业认证图片关联表
            {"lc_trans_rela"},//订单转运关系记录表
            {"lc_user_comp_user"},//企业用户关联表
            {"order"}, //测试分库表
            /**后台运营平台
            */
            {"bs_system_user"},//后台管理系统用户表24
            {"bs_system_resource"},//运营平台资源表25
            {"bs_user_comp"}, // 企业信息表26
            {"bs_user_consignor"}, // 货主信息表27
            {"bs_user_logistics_contact"}, // 物流联系人信息表28
            {"bs_user_communication"}, // 货主沟通信息表29
            {"bs_report_consignor_state"}, // 货主状态变化报表30
            {"bs_carrier_message"}, // 货主状态变化报表31
            {"bs_system_role"}, // 系统角色列表32
            {"bs_user_carrier"}, // 承运商表  33
            {"bs_user_carrier_route"}, // 承运商线路表 34
            {"bs_comp_aptitude"},//资质表35

            {"bs_carrier_contacts"}, // 业务联系人表 36
            {"lc_dic_park"}, // 物流园区表 37
            {"bs_user_carrier_car"}, // 承运商车辆信息表 38
            {"lc_sync_error"}, // 同步异常表39
            {"bs_examine_record"}, // 40
            {"bs_order_base"},//后台订单基本表  41
            {"bs_order_trans"},//后台订单详情表  42
            {"bs_order_trac"},//后台订单跟踪表 43
            {"bs_order_evaluate"}, //后台订单评价表 44
            {"bs_order_trans_record"}, //后台订单运输记录表 45
            {"bs_sync_error"}, //后台同步异常表 46
            {"bs_order_trac_det"},//后台订单跟踪记录表 47
            {"bs_file_store"},//后台文件存储表 48
            {"bs_comp_login_report"},//企业登入记录表 49
            {"bs_order_pub_rela"},//订单发布关联表 50
            {"bs_carrier_message_user"},//消息用户关联表51
            {"lc_sync_order"},//订单同步失败记录表 52
            {"lc_dic_rectype"},//订单同步失败记录表 53
            /**
             * 报价系统
            */
            {"lc_bid_dis"},//城市距离表 54
            {"bs_order_count"}, //订单统计表55
            {"lc_bid_cas"}, //计价阶梯表56
            {"lc_bid_dic"}, //计价字典表57
            {"lc_bid_flt"}, //计价浮动表58
            {"lc_bid_mdl"}, //计价模型表59
            {"lc_bid_sbj"}, //计价科目表60
            {"lc_bid_stg"}, //计价策略表61
            {"lc_bid_svs"},  //计价服务表62
            {"lc_bid_sel"},  //计价服务表63
            {"lc_bid_elm"},  //页面元素表64
            {"bs_carrier_dot"}, //承运商网点表65
            {"lc_bid_vehm"}, //车辆模型表66
            {"lc_bid_sea"}, //淡旺季表67
            {"lc_bid_gtp"}, //货物类型表68
            {"lc_user_comp_frequently"}, //用户企业常用信息对应表69
            {"lc_order_scosts"}, //应收应付表70
            {"lc_order_proc"}, //应收应付构成表71
            {"lc_order_linkups"} //沟通记录72
    };

    //TODO 表名=表在_DB_TABLES常量里固定位置值 ，目的是为了开发人员写代码方便可读性
    //TODO 系统-用户表 0
    public final static int LC_GLOBAL_DICT = 0;
    public final static int LC_GLOBAL_AREA = 1;
    public final static int LC_USER_USER = 2;
    public final static int LC_USER_ROLE = 3;
    public final static int LC_USER_SMS = 4;
    public final static int LC_USER_TOKEN = 5;
    public final static int LC_USER_COMP = 6;
    public final static int LC_USER_COMP_CIRCLE = 7;
    public final static int LC_USER_RES = 8;
    public final static int LC_USER_SMS_TEMPLATE = 9;
    public final static int LC_CARRIER_MESSAGE = 10;
    public final static int LC_CARRIER_PASSAGE_TRANSFER = 11;
    public final static int LC_USER_COMP_ROUTE = 12;
    public final static int LC_USER_ORDER_COUNT = 13;
    public final static int LC_ORDER_BASE = 14;
    public final static int LC_ORDER_TRAC = 15;
    public final static int LC_ORDER_TRAJECTORY = 16;
    public final static int LC_ORDER_TRANS = 17;
    public final static int LC_ORDER_EVALUATE = 18;
    public final static int LC_ORDER_REDUNDANCY = 19;
    public final static int LC_USER_COMP_CREDENTIMG = 20;
    public final static int LC_TRANS_RELA = 21;
    public final static int LC_USER_COMP_USER = 22;
    public final static int ORDER = 23;

    /**
     * 后台运营平台
     */
    public final static int BS_SYSTEM_USER = 24;
    public final static int BS_SYSTEM_RESOURCE = 25;
    public final static int BS_USER_COMP = 26;
    public final static int BS_USER_CONSIGNOR = 27;
    public final static int BS_USER_LOGISTICS_CONTACT = 28;
    public final static int BS_USER_COMMUNICATION = 29;
    public final static int BS_REPORT_CONSIGNOR_STATE = 30;
    public final static int BS_CARRIER_MESSAGE = 31;
    public final static int BS_SYSTEM_ROLE = 32;
    public final static int BS_USER_CARRIER = 33;
    public final static int BS_USER_CARRIER_ROUTE = 34;
    public final static int BS_COMP_APTITUDE = 35;


    public final static int BS_CARRIER_CONTACTS = 36;
    public final static int LC_DIC_PARK = 37;
    public final static int BS_USER_CARRIER_CAR = 38;
    public final static int LC_SYNC_ERROR = 39;
    public final static int BS_EXAMINE_RECORD = 40;
    public final static int BS_ORDER_BASE = 41;
    public final static int BS_ORDER_TRANS = 42;
    public final static int BS_ORDER_TRAC = 43;
    public final static int BS_ORDER_EVALUATE = 44;
    public final static int BS_ORDER_TRANS_RECORD = 45;
    public final static int BS_SYNC_ERROR = 46;
    public final static int BS_ORDER_TRAC_DET = 47;
    public final static int BS_FILE_STORE = 48;
    public final static int BS_COMP_LOGIN_REPORT = 49;
    public final static int BS_ORDER_PUB_RELA = 50;
    public final static int BS_CARRIER_MSG_USER= 51;
    public final static int BS_SYNC_ORDER= 52;
    public final static int LC_DIC_RECTYPE= 53;

    /**
     * 报价系统
     */
    public final static int LC_BID_DIS = 54;
    public final static int BS_ORDER_COUNT= 55;
    public final static int LC_BID_CAS = 56;
    public final static int LC_BID_DIC = 57;
    public final static int LC_BID_FLT = 58;
    public final static int LC_BID_MDL = 59;
    public final static int LC_BID_SBJ = 60;
    public final static int LC_BID_STG = 61;
    public final static int LC_BID_SVS = 62;
    public final static int LC_BID_SEL = 63;
    public final static int LC_BID_ELM= 64;
    public final static int BS_CARRIER_DOT= 65;
    public final static int LC_BID_VEHM= 66;
    public final static int LC_BID_SEA= 67;
    public final static int LC_BID_GTP= 68;
    public final static int LC_USER_COMP_FREQUENTLY = 69;
    public final static int LC_ORDER_SCOSTS= 70;
    public final static int LC_ORDER_PROC= 71;
    public final static int LC_ORDER_LINKUPS= 72;

    // TODO　切分表的规则数组 ：0 代表不需要切分，1 代表按公司模型切分表。
    // 索引必须同_DB_TABLES里的索引对应
    public final static int[] SEG_TABLE_RULE = {
            0, 0, 0, 0, 1,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 1,
            1, 1, 1, 1, 1,
            0, 0, 0, 1, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 1, 1, 1, 1,
            1, 0, 1, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 1,
            1, 1, 1, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0};
}
