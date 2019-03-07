package db.constant;
/**
 * Copyright © 2018空间折叠【FOLDING SPACE】. All rights reserved.
 * @ClassName: BUSConst
 * @Description: TODO 业务相关常量类,除资源、提示信息、SQL之外的信息常量类
 * @version: V1.0
 */
public class BUSConst {
	/**代码中用到的数字常量，避免魔术数字的出现*/
	public static final int _ZERO = 0;
	public static final int _ONE = 1;
	public static final int _TWO = 2;
	public static final int _THREE= 3;
	public static final int _FOUR = 4;
	public static final int _NEGATIVE_ONE = -1;
	public static final int _ELEVEN = 11;
	public static final int _TWELVE = 12;
	/**整型数字类型的取舍范围常量*/
	public static final int _SMALLINTMAX = 65535;
	public static final int _TINYINTMAX = 255;
	/**切分数据库服务器规则：每800个公司共用一台数据库服务器*/
	public static final int _DMNUM = 800;
	/**同一台数据库服务器里切分数据库规则：每100个公司共用一个库，也就是说每台数据库服务器上会有8个数据库*/
	public static final int _MODNUM_EIGHT = 8;
	/**切分表的规则：每二十个公司共用一套表，也就是每一个库里会有5套表*/
	public static final int _MODNUM_FIVE = 5;
	
}
