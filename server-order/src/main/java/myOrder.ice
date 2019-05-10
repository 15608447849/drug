[["java:package:com.hsf.framework.api"]]
#include "cstruct.ice"

/**
 * 我的订单
 */
module myOrder{

    /**
     * 查询信息
     */
    struct QueryParam{
        string origin;               //起始地
        string destination;          //目的地
        string time;                 //时间
        int pageNo;                  //页编号
        int pageSize;                //页size
    };


    /**
     * 一个订单数据
     */
    struct Order{
          string orderno;              //订单编号
          string startc;               //起始地
          string arriarc;              //目的地
          string vtdictc;              //车型
          string vldictc;              //车长
          string pubdate;              //发布日期
          string pubtime;              //发布时间
          string revidate;             //接收日期
          string revitime;             //接收时间
          string ostatus;              //订单状态
          string shipperName;          //货主名
          string shipperPhone;         //货主电话
          string ctdictc;              //货物类型
          int wm;                      //货物重量/体积(量)
          string wmdictc;               //货物重量/体积(单位)
          string puimg;                //取货照片
          string retuimg;              //签收照片
          int puberid;                 //发布人
          int revierid;                //接单人
          int vnum;                      //车数量
          string numdictc;          //件数单位
		  int num;                     //货物数量
      };

    /**
     * 订单列表
     */
    sequence<Order> OrderSeq;

     /**
     * 订单实体
     */
    struct MyOrderICE{
    
	    //发布人ID
	     string puberid;
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
	     string actdatetime;//实际取货时间
	     int tstatus;//订单状态
	
	     int priority; //优先级  0:普通; 1:高(代表优先圈子的人)
	     string revierid;//接单人
	     string compname;//承运商名称
	     string phone;//承运商电话
	     double carriage;//运费
	     string wmdictcn;//货物内容 (重量/体积) 单位
	     string numdictcn;//件数单位
	     string padictcn;//包装要求
	     string ctdictcn;//货物类型
	     string vldictcn;//车长
	     string vtdictcn;//车型
	     string tndictcn;//运输要求
	     string pmdictcn;//货物重量/体积单位
	     string ptdictcn;//付款方式
	     string dmdictcn;//提货方式
	     string redictcn;//是否返单
	     bool istrans;//是否为转运订单
	     string pubcompid;//发布人企业id
	     string ext1;//扩展字段1(接单企业码)
	     string ext2;//扩展字段2(货物名称)
	     string ext3;//扩展字段3(货物照片)
	     string source;//数据来源(0:不确定; 1: app; 2: 平台bs版; 3: 微信公众号)
	     cstruct::stringSeq feedArr;//费用
	     int optinfoType;//0 保存 1 保存并下单
	     string pubername;
	     string remark;//订单备注
    };


    /**
     * 订单查询返回结构
     */
    struct OrderReturnData{
        int code;                     //返回码
        string msg;                   //返回信息
        int pageNo;                   //页编号
        string total;                 //查询总数
        int pageSize;                 //页size
        OrderSeq orderList;           //订单列表
    };

    /**
     * 地区结构
     */
    struct AreaData{
        string areac;                  //地区码
        string value;                  //快查码
        string label;                  //地区名
		string children;			   //下一级地区
    };

     sequence<AreaData> AreaDataSeq;

    /**
     * 行程单轨迹
     */
    struct OrderTrajectory{
        string orderid;               //订单编号
        string createdata;            //创建日期
        string createtime;            //创建时间
        string trajectoryrecord;      //行程纪录
    };

    /**
    * 走货痕迹
    */
    struct TracOfOrder{
        string orderno;//订单号
        int trancompid;//公司id
        int driverid;//
        string content;//内容
        int cstatus;//综合状态码 （0：启动，1：删除，32：走货轨迹，64：地图轨迹）
    };

    /**
     * 订单接收返回对象。
     */
    struct ReceiveOrderReturnData {
        int code;    //返回码 (0：OK 其他:Bye)
        string msg; //返回信息 (成功则返回生成的取货码，否则为出错信息。)
    };

    /**
    * 评价信息
    */
    struct OrderEvaluate {
        string orderid;//订单id
        int pubcompid;//发布企业
        int evaluator;//评价人
        int revicompid;//运输企业
        int revierid;//运输司机
        int grade;//评价等级
        int service;//服务态度
        int timely;//运输时效
        int quality;//运输质量
        string remarks;//描述
        int cstatus;//综合状态码
        string picurlarr;//图片评价地址
    };
     sequence<OrderEvaluate> OrderEvaluateSeq;

     /**
     * 订单详情数据格式
     */
     struct OrderInfoDetail{
       MyOrderICE orderifo;//订单信息
       OrderEvaluate ordereva;//评价详情
       string logPath;
     };

     /**
       * 订单生成转运码
       */
     struct OrderTrancQR{
         string trancCode;//转运码地址
         string startc;//起始地编码
         string startcn;//起始地名称
         string arriarc;//到达地编码
         string arriarcn;//到达地名称
         int num;//数量
         string numdict;//数量单位
         string numtxt;//数量+单位文本
     };

    struct Linkup {
        int oid;
        string orderno;
        int ctype; //沟通类型
        int citem; //沟通事项   
        string content; //沟通内容
        double eprice; //预期价
        string linkpho; //沟通人联系电话
        string linker; //联系人
        string cdate; //沟通日期   
        string ctime; //沟通时间
        int opt; //操作人
        string optname; //操作人
        int pcompid; //发布企业
        int cstatus;
    };
    
    sequence<Linkup> LinkupSeq;
    
    struct AddressInfo {
        string name;
        string phone;
        int citycode;
        string address;
        
        string cityname;
    };


    struct QueryOrder {
    // trans START -------
        int oid;
        string orderno;
        int pubcompid;
        int puberid;
        string pubdate;
        string pubtime;
        string phone1;
        string phone2;
        int ptdictc;
        double carriage;
        int startc;
        string startaddr;
        string puimg;
        string pusdate;
        string pustime;
        string puedate;
        string puetime;
        int revicompid;
        int revierid;
        string revidate;
        string revitime;
        int cstatus;
        string actdelidate;
        string actdelitime;
    // trans END -------

    // base START -------
        int arriarc;
        string arriaddr;
        int ctdictc;
        int num;
        int numdictc;
        double wm;
        int wmdictc;
        int vnum;
        int vldictc;
        int vtdictc;
        double insureamt;
        double codamt;
        string consignee;
        string consphone;
        int redictc;
        string easdate;
        string eastime;
        string eaedate;
        string eaetime;
        string arridate;
        string arritime;
        string retuimg;
        int source;
        int bstatus;
    // base END -------
    
    // scost START -------
        int scostoid;
        int fstatus;
    // scost END -------
        
    /* 非trans表中的tstatus 实际判定为：IF(ostatus > 0, ostatus, tstatus) */
        int tstatus;
        
    // SP START -------    
        string pubphone;
        string pubcompname;
        string vtdictn;
        string vldictn;
        string arriarn;
        string startn;
        string wmdictn;
        string ctdictn;
        string redictn;
        string ptdictn;
        string numdictn;
    // SP END -------  
    };

    /**
     * 我的订单服务接口
     */
    interface MyOrderServer{
        /**
         * 获取行程单轨迹(我发布的订单的行程地图的)
         */
        string getOrderTrajectory(cstruct::UserParam userParam, int compid, string orderId);

        /**
         * 获取行程单轨迹
         * orderid:订单号 type:0-走货轨迹 1-地图轨迹
         */
        string getOrderTraByOrderid(cstruct::UserParam userParam, cstruct::stringSeq ordernos, int type);

        /**
         * 获取地区数据
         */
        AreaData getAreaData(string areac);

        /**
        * 接收行程轨迹
        * orderid:订单id, compid:公司id, driverphone:电话号码， traveljson：行程轨迹json
        */
        int acceptTravel(long orderid, int compid, long driverphone,string traveljson);

        /**
        * 接收行程纠偏轨迹
        * orderid:订单id, compid:公司id, driverphone:电话号码， traveljson：行程轨迹json
        */
        int acceptTravelCorrect(long orderid, int compid, long driverphone,string traveljson);

        /**
        * 录入行程
        * tracinfo：录入行程对象
        */
        string entryTravel(TracOfOrder tracinfo);


		/*
         * 拒绝订单
         */
        string refuseOrder(cstruct::UserParam userParam, string orderno);
        
        /**
         * 接收订单
         */
        string receiveOrder(cstruct::UserParam userParam, string orderno);
        
        
        /**
        * 根据市查询省下所有城市
        */
        AreaDataSeq getTransferPoint(int place);
      
        /**
        * 根据发布企业、订单号获取取货码。
        */
        string getPickCode(cstruct::UserParam userParam, string orderno);
        
        /**
        * 验证取货密码。
        */
        string checkPickPwd(cstruct::UserParam userParam, string orderno, string pickPwd);

        /**
        * 评价
        */
        string insertOrderEvaluate(OrderEvaluate orderEvaluate);

        /**
        * 确认签收——（状态变为待评价）
        * orderid:订单号 uoid:用户id
        */
        string conReceipt(string orderid, cstruct::UserParam userParam);

        /**
        * 修改返回订单数据
        * orderid:订单号 type:0:我发布的 1：我接收的
        */
        string getOrderDetail(string orderid, cstruct::UserParam userParam, int type);

        /**
        * 重新发布获取详情
        */
        string getOrderDetailByRepub(string orderid, cstruct::UserParam userParam, int type);

        /**
        * 修改订单(重新发布)
        */
        string updateMyPublishOrder(MyOrderICE order, cstruct::UserParam userParam);

        /**
         * 订单转运
         */
         string transOrder(MyOrderICE order, cstruct::UserParam userParam);

         /**
         * 双击查看订单详情
         * type:0:我发布的 1：我接收的
         */
         string getOrderInfo(string orderid, cstruct::UserParam userParam, int type);

         /**
         * 刷新订单
         */
         string flushOrder(cstruct::UserParam userParam, string orderid);
         
         /**
         * 取消订单发布
         */
         string cancelOrder(string orderid, cstruct::UserParam userParam);
         
         /**
         * 重新发布订单
         */
         string repubOrder(string orderid, cstruct::UserParam userParam);
         
         /**
         * 关闭订单
         */
         string closeOrder(string orderid, cstruct::UserParam userParam);

         /**
         * 取消抢单
         */
         string cancelRobbing(string orderid, cstruct::UserParam userParam);

         /**
         * 获取企业评价
         */
         OrderEvaluateSeq getOrderEvaluate(int pubcompid);

         /**
         * 根据发布企业、订单号、订单状态(lzp需要)生成转运码
         */
         string getTrancCode(cstruct::UserParam userParam, string orderno, int tstatus);
         
         cstruct::intSeq getAllReceiver(string order);
         
         void syncOrderFailReport(string orderno, int compid);
         
         /**
         * 模糊匹配查询承运商
         */
         string queryCompByName(cstruct::UserParam userParam, string compName, string orderno);
        
        /**
         * 删除我的已发布订单(微信端) <br>
         * param oid:被删除的数据的OID <br>
         * param type:删除操作的来源
         */
        string deleteOrder(cstruct::UserParam userParam, string orderno, int type); 
        
        /**
        * 查询沟通记录
        */
        string queryLinkups(cstruct::UserParam userParam, int pubCompid, string orderno, cstruct::stringSeq queryParam, cstruct::Page page, out cstruct::Page pageOut);
        
        /**
        * 新增沟通记录
        */
        string addLinkups(cstruct::UserParam userParam, Linkup linkup);
        
        /**
        * 下单失败
        */
        string orderFailed(cstruct::UserParam userParam, int pubCompid, string orderno,int cstatus);
        
        /**
        * 智能匹配收/发货人信息。
        * status 0：查询发 1：查询收
        * params [姓名, 手机]
        */
        string matchAddressInfo(cstruct::UserParam userParam, int status, cstruct::stringSeq params);
        
        /**
        * 确认收费
        * payer 同查询的payer字段。(支付方)
        */
        string feeReceived(cstruct::UserParam userParam, int payer, string orderno);
        
        /**
        * 更新回单状态
        * bstatus ：已签 = 1, 已收 = 2, 已发 = 3
        */
        string billSigned(cstruct::UserParam userParam, int pubcompid, string orderno, int bstatus);
        
        /**
         * 查询我的已接受订单
         */
        string queryMyRecvOrder(cstruct::UserParam userParam, string params, cstruct::Page page, out cstruct::Page pageOut);

        /**
         * 查询我的已发布订单
         */
        string queryMyPubOrder(cstruct::UserParam userParam, string params, cstruct::Page page, out cstruct::Page pageOut);
        
         /**
         * 查询企业动态
         */
        string queryEnterpriseDynamics(int pubcompid, cstruct::Page page, out cstruct::Page pageOut);

        /**
        * 根据订单号查询最后承运商名称（一块医药专用）
        */
        string getCarrierName(long orderno, int pubcompid);
    };

};
