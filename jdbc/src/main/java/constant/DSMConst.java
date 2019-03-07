package db.constant;

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
	        {"t_area"}, // 地区表
	        {"t_comp"}, // 企业信息表
	        {"t_comp_aptitude"}, // 企业资质
	        {"t_comp_user"}, // 用户企业对应表
	        {"t_product"}, // 产品信息表
	        {"t_product_via_village"}, // 公司表
	        {"t_schedule"}, //行程表
	        {"t_traveler_list"}, //游客表
	        {"t_user"}//用户表
	};

	//TODO 表名=表在_DB_TABLES常量里固定位置值 ，目的是为了开发人员写代码方便可读性
	//TODO 系统-用户表 0
    public final static int T_AREA = 0;
    public final static int T_COMP = 1;
    public final static int T_COMP_APTITUDE = 2;
    public final static int T_COMP_USER = 3;
    public final static int T_PRODUCT = 4;
    public final static int T_PRODUCT_VIA_VILLAGE = 5;
    public final static int T_SCHEDULE = 6;
    public final static int T_TRAVELER_LIST = 7;
    public final static int T_USER = 8;

	public final static int[] SEG_TABLE_RULE = { 0, 0, 0, 0, 0, 0, 0, 0, 0};
}

