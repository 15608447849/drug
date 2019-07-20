package redis.provide;

import IceInternal.Ex;
import redis.clients.jedis.*;
import redis.clients.util.JedisClusterCRC16;

import java.util.*;

/**
 * Redis通用提供类
 * 
 * @author JiangWenGuang
 * @since 2018-06-14
 * @version 1.0
 *
 */
public class RedisProvide{
	
	protected static JedisCluster jedisCluster;

	static{
		JedisPoolConfig config = JedisPoolConfigFactory.factory.getConfig();
		String [] hosts = JedisPoolConfigFactory.getHosts();
		JedisClusterFactory clusterFactory = new JedisClusterFactory(config, hosts);
		jedisCluster = clusterFactory.getJedisCluster();
	}
	
	public RedisProvide() {

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
		Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
		for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
			Jedis jedis = null;
			JedisPool jedisPool = null;
			try{
				jedisPool = entry.getValue();
				jedis = jedisPool.getResource();
				// 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)
				if (!jedis.info("replication").contains("role:slave")) {
					Set<String> keys = jedis.keys(keyStartWith + "*");
					if (keys.size() > 0) {
						Map<Integer, List<String>> map = new HashMap<>();
						for (String key : keys) {
							// cluster模式执行多key操作的时候，这些key必须在同一个slot上，不然会报:JedisDataException:
							// CROSSSLOT Keys in request don't hash to the same slot
							int slot = JedisClusterCRC16.getSlot(key);
							// 按slot将key分组，相同slot的key一起提交
							if (map.containsKey(slot)) {
								map.get(slot).add(key);
							} else {
								List<String> list = new ArrayList<>();
								list.add(key);
								map.put(slot, list);
							}
						}
						for (Map.Entry<Integer, List<String>> integerListEntry : map.entrySet()) {
							jedis.del(integerListEntry.getValue().toArray(new String[integerListEntry.getValue().size()]));
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally {
				if(jedis != null){
					jedis.close();
				}
			}


		}
	}

	/**
	 * 获取前缀为keyStartWith的所有键
	 *
	 * @param keyStartWith
	 */
	public List<String> getRedisKeyStartWith(String keyStartWith){
		List<String> keyList = new ArrayList<>();
		Map<String, JedisPool> clusterNodes = jedisCluster.getClusterNodes();
		for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
			Jedis jedis = null;
			JedisPool jedisPool = null;
			try {
				jedisPool = entry.getValue();
				jedis = jedisPool.getResource();
				// 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)
				if (!jedis.info("replication").contains("role:slave")) {
					Set<String> keys = jedis.keys(keyStartWith + "*");
					if (keys.size() > 0) {
						Map<Integer, List<String>> map = new HashMap<>();
						for (String key : keys) {
							keyList.add(key);
						}
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}finally {
				if(jedis != null){
					jedis.close();
				}
			}

		}
		return keyList;

	}

//	public JedisCluster getJedisCluster(){
//		jedisCluster.
//		return jedisCluster;
//	}
}
