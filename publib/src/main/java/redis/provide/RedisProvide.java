package redis.provide;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.List;

/**
 * Redis通用提供类
 * 
 * @author JiangWenGuang
 * @since 2018-06-14
 * @version 1.0
 *
 */
public class RedisProvide{
	
	protected JedisCluster jedisCluster;
	
	public RedisProvide() {
		JedisPoolConfig config = JedisPoolConfigFactory.factory.getConfig();
		String [] hosts = JedisPoolConfigFactory.getHosts();
		JedisClusterFactory clusterFactory = new JedisClusterFactory(config, hosts);
		jedisCluster = clusterFactory.getJedisCluster();
	}

	public RedisProvide(JedisClusterFactory jedisClusterFactory){
	       jedisCluster = jedisClusterFactory.getJedisCluster();
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

	/**
	 * 持久保存
	 * 
	 * @param key
	 * @return
	 */
	public Object persist(String key){
	    return jedisCluster.persist(key);        
	}
	
	/**
	 * 设置 key的过期时间以【秒】计
	 * 
	 * @param key
	 * @param seconds 单位【秒】
	 */
	public Long expire(String key,int seconds) {
		return jedisCluster.expire(key, seconds);
	}
	
	/**
	 * 设置 key的过期时间以【毫秒】计
	 * 
	 * @param key
	 * @param milliseconds 单位【毫秒】
	 */
	public Long pexpire(String key,Long milliseconds) {
		return jedisCluster.pexpire(key, milliseconds);
	}
	
	/**
	 * key剩余生存时间
	 * 
	 * @param key
	 * @return 单位【秒】.永久生存或者不存在的都返回-1
	 */
	public Long remainingSurvivalTime(String key) {
		return jedisCluster.ttl(key);
	}
	

	
	/**
	 * 查看key所储存的值的类型
	 * 
	 * @param key
	 * @return
	 */
	public String typeOf(String key) {
		return jedisCluster.type(key);
	}

	/**
	 * 删除前缀为keyStartWith的所有键
	 *
	 * @param keyStartWith
	 */
	public void deleteRedisKeyStartWith(String keyStartWith){
		ScanParams scanParams = new ScanParams();
		scanParams.match(keyStartWith+"*");
		ScanResult<String> result = jedisCluster.scan("0", scanParams);
		List<String> keys = result.getResult();
		for (String key : keys){
			System.out.println(key);
		}
		jedisCluster.del(keys.toArray(new String[keys.size()]));
	}
}
