[["java:package:com.hsf.framework"]]
#include "cstruct.ice"
/**
 * 订单ice
 */
module order{

	struct FilterConditionICE{
		// 全文关键字
		string keyword;
		// 起始地地址
		string startAddr;
		// 终止地地址
		string destAddr;
		// 货物类型
		string goodsType;
		// 货车类型
		string vt;
		// 货物体积/重量
		string wm;
		// 车长
		string vlen;
		// 查询最小费用
		string minCost;
		// 查询最大费用
		string maxCost;
		// 查询起始时间
		string startTime;
		// 查询结束时间
		string endTime;
		// 订单状态
		string ostatus;
		// 最大时间
		string maxTime;
	};

	 /**
	 * 订单详细信息(信息大厅,我的圈子)
	 */
	 struct OrderDetail{
	 	string time;//时间
	 	string ostatus;//订单状态
	 	string cost; // 运费
	 	string wm; // 货物体积/重量
	 	string vlen; // 车长
	 	string startAddr; // 起始地地址
	 	string destAddr; // 到达地地址
	 	string id; // 订单号
	 	string vt; // 货车类型
	 	string goodsType; // 货物类型
	 	string pubername; // 发布人
	 	string insureamt; // 声明保价金额(元)
	 	string codamt; // 声明保价金额(元)
	 	string payType; // 付款方式
	 	string pustime; // 取货开始时间
	 	string puetime; // 取货结束时间
	 	string puberid; // 发布人用户id
	 	string pubercompid; // 发布人企业id
	 	string rank; // 推荐等级
	 };
	 
	 /**
	 * 订单详细信息集合模型
	 */
	 sequence<OrderDetail> OrderSeq;
	 
	 /**
	 * 订单实体
	 */
	 struct OrderICE{
	 	//发布人ID
	 	string puberid;
	 	// 发布人企业id
	 	string pubercompid; 
	 	// 发布人
	 	string pubername; 
	 	//发布人承运商名
	 	string puberCarrier;
	 	//联系电话
	 	string phone1;
	 	//联系电话
	 	string phone2;
	 	//发布时间
	 	string pubdatetime; //yy-MM-dd HH:mm:ss
	 	//TMS运单号
	 	string billno; 
	 	//自动生成的订单号
	 	string orderno;

	 	//出发地 : (详情)省#市#区 (保存)区码
	 	string startc;
	 	// 出发地: 出发地详细地址
	 	string startaddr;
	 	//目的地 : (详情)省#市#区 (保存)区码
	 	string arriarc;
	 	// 目的地 : 出发地详细地址
	 	string arriaddr;
	 	// 额外字段 出发地: (移动端)省#市#区
	 	string startcext;
	 	// 额外字段  地: (移动端)省#市#区
	 	string arriarcext;

	 	//货物内容 (重量/体积) 数量
	 	double wm;
	 	//货物内容 (重量/体积) 单位 字符串
	 	string wmdictc;
	 	//单位数量
	 	int num;
	 	//单位数量 单位 字符串
	 	string numdictc;
	 	//包装要求 字符串
		string padictc; 
		//货物类型 字符串
		string ctdictc;

		
		//车数量(台)
		 int vnum ;
		//车长字典码 字符串
		string vldictc;
		//车型字典码 字符串
		string vtdictc;
		//运输要求字典码 字符串
		string tndictc;
		// 包装要求数组
		cstruct::intSeq tndictarr;

		//货物运费
		double price;
		// 运费度量单位
		string pmdictc;
		//代收货款金额
		double codamt;
		//声明保价
		double insureamt;
		//付款方式 字符串
		string ptdictc;
		// 实际运费(元)
		double carriage;

		//收货人
		string consignee;
		//手机号码
		string consphone;
		//送货方式 str
		string dmdictc;
		//是否返单 str
		string redictc;
		//取货时间开始 yyyy-MM-dd hh:mm:ss
		string pusdatetime; 
		//取货结束时间 yyyy-MM-dd hh:mm:ss
		string puedatetime;
		//期望到货时间起始 yyyy-MM-dd hh:mm:ss
		string easdatetime;
		//期望到货结束时间 yyyy-MM-dd hh:mm:ss
		string eaedatetime;
		// 抢单时间yyyy-MM-dd hh:mm:ss
		string revidatetime;
		// 签收/到货时间yyyy-MM-dd hh:mm:ss
		string arridatetime;

		//订单状态 
		int ostatus;//订单状态
		
		int priority; //优先级  0:普通; 1:高(代表优先圈子的人)	
		
		// 货物名称
		string cargoname;
		// 货物图片地址
		string cargoimg;
		// 数据来源  0:不确定; 1: app; 2: 平台bs版; 3: 微信公众号
		int source;
	 };
	 
	 
	/**
	 * 订单服务接口
	 */
	interface OrderService {
	
	     /**
		 * 生成订单序号
		 */
		 string generateOrderNo();
	
		 /**
		 * 全文检索出当天的订单
		 */
		 string queryOrderByCurdate(cstruct::UserParam userparam,string pagenum,string pagesize,FilterConditionICE params);
		 
		 /**
		 * 全文检索出当天的圈子订单
		 */
		 string queryCircleOrderByCurdate(cstruct::UserParam userparam,string pagenum,string pagesize,FilterConditionICE params);
		 
		 /**
		 * 全文检索出当天的订单(App)
		 */
		 string queryAppOrderByCurdate(cstruct::UserParam userparam,cstruct::stringSeq params);
		 
		  /**
		 * 全文检索出当天的圈子订单
		 */
		 string queryAppCircleOrderByCurdate(cstruct::UserParam userparam,cstruct::stringSeq params);
		 
		/**
	 	*添加订单
	 	*/
	 	string addOrder(cstruct::UserParam userparam,OrderICE params);
	 	
	 	/**
	 	*获取订单详情
	 	*/
	 	string getOrderDetail(cstruct::stringSeq params);
	 	
	 	/**
	 	*抢单
	 	*/
	 	string robbingOrder(cstruct::UserParam userparam,cstruct::stringSeq params);
	 	
	};
};