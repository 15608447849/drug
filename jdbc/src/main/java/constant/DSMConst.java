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
            {"d_system_user"}, // 用户表
            {"lc_global_dict"}, // 用户表
            {"d_system_role"}, // 角色表
            {"d_system_resource"}, // 资源表
            {"d_order_base"},
            {"d_order_product"},
            {"d_sms_template"},//短信模板
            {"d_comp_ship_info"},//收货人

    };

    //TODO 表名=表在_DB_TABLES常量里固定位置值 ，目的是为了开发人员写代码方便可读性
    //TODO 系统-用户表 0
    public final static int D_SYSTEM_USER = 0;
    public final static int LC_GLOBAL_DICT = 1;
    public final static int D_SYSTEM_ROLE = 2;
    public final static int D_SYSTEM_RESOURCE = 3;
    public final static int D_ORDER_BASE = 4;
    public final static int D_ORDER_PRODUCT = 5;
    public final static int D_SMS_TEMPLATE = 6;
    public final static int D_COMP_SHIP_INFO = 7;







    // TODO　切分表的规则数组 ：0 代表不需要切分，1 代表按公司模型切分表。
    // 索引必须同_DB_TABLES里的索引对应
    public final static int[] SEG_TABLE_RULE = {
            0, 0, 0, 0, 1,
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
}
