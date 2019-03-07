package global;
/**
 * Copyright © 2018空间折叠物流科技【FSPACE】. All rights reserved.
 * @ClassName: QCParam
 * @Description: TODO通用查询条件对象，用于后台组装查询条件及动态查询SQL条件。
 * @author: shanben-CN EMAIL：shanben11@qq.com
 * @date: 2018年4月14日 上午11:22:53
 * @version: V1.0
 */
public class QCParam {
	/**参数数据类型：0－日期到天；1－日期到秒；2－int；3－long；4：字符串*/
	private int pdt = 4;
	/**参数数据值*/
	private String pdv;
	/**参数对应的查询SQL里？的个数，如果对应多个，则要求SQL里此多个？必须是连续*/
	private int matchPN = 1;
	/**参数对应的动态查询变量索引值。*/
	private int dyQCSQLIndex = 0;
	
	/**参数数据类型：0－日期到天*/
	public static final int PDT_DATA = 0;
	/**参数数据类型：1－日期到秒*/
	public static final int PDT_DATATIME = 1;
	/**参数数据类型：2－int*/
	public static final int PDT_INT = 2;
	/**参数数据类型：3－long*/
	public static final int PDT_LONG= 3;
	/**参数数据类型：4：字符串*/
	public static final int PDT_STRING = 4;
	
	public QCParam(String pdv){
		this.pdv = pdv;
	}
	public QCParam(int pdt,String pdv){
		this.pdt = pdt;
		this.pdv = pdv;
	}
	public QCParam(String pdv,int matchPN,int dyQCSQLIndex){
		this.pdv = pdv;
		this.matchPN = matchPN;
		this.dyQCSQLIndex = dyQCSQLIndex;
	}
	public QCParam( int pdt,String pdv,int matchPN,int dyQCSQLIndex){
		this.pdt = pdt;
		this.pdv = pdv;
		this.matchPN = matchPN;
		this.dyQCSQLIndex = dyQCSQLIndex;
	}
	public int getPdt() {
		return pdt;
	}
	public void setPdt(int pdt) {
		this.pdt = pdt;
	}
	public String getPdv() {
		return pdv;
	}
	public void setPdv(String pdv) {
		this.pdv = pdv;
	}
	public int getMatchPN() {
		return matchPN;
	}
	public void setMatchPN(int matchPN) {
		this.matchPN = matchPN;
	}
	public int getDyQCSQLIndex() {
		return dyQCSQLIndex;
	}
	public void setDyQCSQLIndex(int dyQCSQLIndex) {
		this.dyQCSQLIndex = dyQCSQLIndex;
	}
}
