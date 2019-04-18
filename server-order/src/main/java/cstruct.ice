[["java:package:com.hsf.framework.api"]]

module cstruct{
	struct BoolMessage{ 
		bool flag;
		string smessage="操作成功"; //成功返回信息
		string fmessage="操作失败"; //失败返回信息
	};
	
	 /** 分页对象 */
	 struct Page{
		int pageSize;	
		int pageIndex;	
		int totalItems;
		int totalPageCount;
	};

    /**
    * @descrption:新增公用
    * @auth:jiangwenguang
    * @time:2018/09/04
    * @version:
    */
    struct UserParam{
		string uid; // 用户id
		string compid;	// 公司id
	};

     /**long类型集合模型*/
	 sequence<long> longSeq;
	 
	 /**int类型集合模型*/
	 sequence<int> intSeq;
	 
	 sequence<byte> byteSeq;

	 sequence<string> stringSeq;
	 
	 sequence<double> doubleSeq;
};