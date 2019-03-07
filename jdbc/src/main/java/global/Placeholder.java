package db.global;

/** 
 * @version 版本：1.0 
 * {@code} 占位符类，用来传递动态参数。如：多表同一事务新增时，一个表的某字段需要插入另一个表刚新增的记录的自增值时。
 */
public class Placeholder {
	/**需要返回自增长字段值集合List<Object[]>里List集合索引值*/
	private int listIndex = 0;
	/**需要返回自增长字段值集合List<Object[]>里Object数组的索引值*/
	private int objectIndex = 0;
	/**默认构造函数，用于自增长字段仅是单表单记录时的情况，其他情况请用其他构造函数。*/
	public Placeholder(){}
	
	public Placeholder(int listIndex,int objectIndex){
		this.listIndex = listIndex;
		this.objectIndex = objectIndex;
	}
	
	public int getListIndex() {
		return listIndex;
	}
	public void setListIndex(int listIndex) {
		this.listIndex = listIndex;
	}
	public int getObjectIndex() {
		return objectIndex;
	}
	public void setObjectIndex(int objectIndex) {
		this.objectIndex = objectIndex;
	}
}
