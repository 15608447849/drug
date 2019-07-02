package redis.provide;

/**
 * Redis 字符串数据类型的相关命令用于管理 redis字符串值
 * 
 * @author JiangWenGuang
 * @since 2018-06-14
 * @version 1.0
 *
 */
public class RedisStringProvide extends RedisProvide{

	public RedisStringProvide() {
		super();
	}
	
	public RedisStringProvide(JedisClusterFactory jedisClusterFactory) {
		super(jedisClusterFactory);
	}

	/**
	 * 根据键获取值
	 * 
	 * @param key 键
	 * @return
	 */
	public String get(String key){
		return jedisCluster.get(key);   
	}
	
	/**
	 * 将 key中储存的数字值增一
	 * 
	 * @param key
	 * @return
	 */
	public Long increase(String key) {
		return jedisCluster.incr(key);
	}
	
	/**
	 * 将 key所储存的值加上给定的增量值
	 * 
	 * @param key
	 * @param increment 增量值
	 * @return
	 */
	public Long increase(String key, int increment) {
		return jedisCluster.incrBy(key, increment);
	}
	
	
	/**
	 * 将 key中储存的数字值减一
	 * 
	 * @param key
	 * @return
	 */
	public Long decrease(String key) {
		return jedisCluster.decr(key);
	}
	
	/**
	 * key所储存的值减去给定的减量值
	 * 
	 * @param key
	 * @param decrement 减量值
	 * @return
	 */
	public Long decrease(String key, int decrement) {
		return jedisCluster.decrBy(key, decrement);
	}
	
	/**
	 * 返回 key所储存的字符串值的长度
	 * 
	 * @param key 键
	 * @return
	 */
	public Long length(String key){
	    return jedisCluster.strlen(key);        
	}
	
	/**
	 * 设置指定 key的值
	 * 
	 * @param key 键
	 * @param obj 元素值
	 * @return OK:代表设置成功
	 */
	public String set(String key, Object obj) {
		jedisCluster.del(key);
	    return jedisCluster.set(key, obj.toString());
	}

	/**
	 * 只有在 key 不存在时设置 key的值。
	 * 
	 * @param key 键
	 * @param obj 元素值
	 */
	public void setByKeyNotExist(String key, Object obj) {
	    jedisCluster.setnx(key, obj.toString());
	}
	
	/**
	 * 直接在原来的值追加新的内容
	 * 
	 * @param key 键
	 * @param appendVal 追加的新值
	 */
	public void append(String key, Object appendVal) {
	    jedisCluster.append(key, appendVal.toString());
	}
	
	
	/**
	 * 根据键删除值
	 * 
	 * @param key 键
	 * @return 0:代表删除失败
	 */
	public Long delete(String key) {
	    return jedisCluster.del(key);
	}

}
