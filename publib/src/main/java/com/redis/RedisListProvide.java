package com.redis;

import java.util.List;

/**
 * Redis列表是简单的字符串列表，按照插入顺序排序。你可以添加一个元素到列表的头部（左边）或者尾部（右边）一个列表最多可以包含 4294967295 个元素 (每个列表超过40亿个元素)。

 * @author JiangWenGuang
 * @since 2018-06-14
 * @version 1.0
 * 
 * @param <T>
 */
public class RedisListProvide<T> extends RedisProvide {

	public RedisListProvide() {
		super();
	}
	
	public RedisListProvide(JedisClusterFactory jedisClusterFactory) {
		super(jedisClusterFactory);
	}

	/**
	 * 添加列表头部元素
	 * 
	 * @param colname 集合名
	 * @param val 元素值
	 * @return 0:添加失败 1:添加成功
	 */
	public Long addHeadElement(String colname,T val) {
		return jedisCluster.lpush(colname, val.toString());
	}
	
	/**
	 * 添加列表尾部元素
	 * 
	 * @param colname 集合名
	 * @param val 元素值
	 * @return 0:添加失败 1:添加成功
	 */
	public Long addEndElement(String colname,T val) {
		return jedisCluster.rpush(colname, val.toString());
	}
	
	/**
	 * 删除头部元素
	 *  
	 * @param colname 集合名
	 * @return 移除的元素值
	 */
	public String removeHeadElement(String colname) {
		return jedisCluster.lpop(colname);
	}
	
	/**
	 * 删除尾部元素
	 *  
	 * @param colname 集合名
	 * @return 移除的元素值
	 */
	public String removeEndElement(String colname) {
		return jedisCluster.rpop(colname);
	}
	
	/**
	 * 根据索引更新集合中元素值
	 * 
	 * @param colname 集合名
	 * @param index 索引值
	 * @param val 更新后的元素值
	 */
	public String updateElementByIndex(String colname,int index,T val) {
		return jedisCluster.lset(colname, index, val.toString());
	}
	
	/**
	 * 根据元素值删除集合中包含该元素值所有元素
	 * 
	 * @param colname 集合名
	 * @param val 元素值
	 * @return 0:代表删除失败
	 */
	public Long deleteElementByVal(String colname, T val) {
		List<String> list = getAllElements(colname);
		int num = 0;
		if(list != null && list.size() > 0) {
			for(String v : list) {
				if(v.equals(val.toString())) {
					num++;
				}
			}
		}
		return jedisCluster.lrem(colname, num, val.toString());
	}
	
	/**
	 * 根据元素值和删除个数删除集合中包含该元素值元素
	 * 
	 * @param colname 集合名
	 * @param delNum 删除的个数(有重复时)
	 * @param val 元素值
	 * @return 0:代表删除失败
	 */
	public Long deleteElementByNumAndVal(String colname,int delNum, T val) {
		return jedisCluster.lrem(colname, delNum, val.toString());
	}
	
	/**
	 * 获取所有的元素
	 * 
	 * @param colname 集合名
	 * @return
	 */
	public List<String> getAllElements(String colname) {
		return jedisCluster.lrange(colname, 0, -1);
	}
	
	/**
	 * 获取列表的长度
	 * 
	 * @param colname 集合名
	 * @return
	 */
	public Long size(String colname) {
		return jedisCluster.llen(colname);
	}

}
