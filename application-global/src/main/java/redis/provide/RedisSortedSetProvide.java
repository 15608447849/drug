package redis.provide;

import java.util.Set;

/**
 * Redis 有序集合和集合一样也是string类型元素的集合,且不允许重复的成员。不同的是每个元素都会关联一个double类型的分数。redis正是通过分数来为集合中的成员进行从小到大的排序。
 * 有序集合的成员是唯一的,但分数(score)却可以重复。集合是通过哈希表实现的，所以添加，删除，查找的复杂度都是O(1)。 集合中最大的成员数为 4294967295 (每个集合可存储40多亿个成员)。
 *
 * @author JiangWenGuang
 * @since 2018-06-14
 * @version 1.0
 * 
 * @param <T>
 */
public class RedisSortedSetProvide<T> extends RedisProvide {

	public RedisSortedSetProvide() {
		super();
	}
	
	public RedisSortedSetProvide(JedisClusterFactory jedisClusterFactory) {
		super(jedisClusterFactory);
	}

	/**
	 * 添加元素
	 * 
	 * @param colname 集合名
	 * @param score 分数(元素权重)
	 * @param val 元素值
	 * @return 0:添加失败 1:添加成功
	 */
	public Long addElement(String colname, double score, T val) {
		return jedisCluster.zadd(colname, score, val.toString());
	}
	
	/**
	 * 根据元素值删除元素
	 * 
	 * @param colname 集合名
	 * @param val 元素值
	 * @return 0:代表删除失败
	 */
	public Long delElement(String colname, T val) {
		return jedisCluster.zrem(colname, val.toString());
	}
	
	/**
	 * 获取集合所有元素的个数
	 * 
	 * @param colname 集合名
	 * @return 个数
	 */
	public Long size(String colname) {
		return jedisCluster.zcard(colname);
	}
	
	/**
	 * 在分数区间内统计元素的个数
	 *  
	 * @param colname 集合名
	 * @param minscore 最小分数(元素权重)
	 * @param maxscore 最大分数(元素权重)
	 * @return 个数
	 */
	public Long countByScoreRange(String colname,double minscore, double maxscore) {
		return jedisCluster.zcount(colname, minscore, maxscore);
	}
	
	/**
	 * 根据元素值获取元素的分数值(元素权重)
	 * 
	 * @param colname 集合名
	 * @return 个数
	 */
	public Double getScoreByVal(String colname, T val) {
		return jedisCluster.zscore(colname, val.toString());
	}
	
	/**
	 * 获取所有元素
	 * 
	 * @param colname 集合名
	 * @return
	 */
	public Set<String> getAllElements(String colname) {
		return jedisCluster.zrange(colname, 0, -1);
	}
	
	
}
