package com.redis.provide;

import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Reids集群工厂
 * 
 * @author Administrator
 * @since 2018-06-14
 * @version 1.0
 *
 */
public class JedisClusterFactory {
	
	private JedisCluster jedisCluster;

	public JedisCluster getJedisCluster() {
	    return jedisCluster;
	}

	public JedisClusterFactory(JedisPoolConfig jedisPoolConfig,String [] hosts){    
	    
		Set<HostAndPort> jedisClusterNodes= new HashSet<HostAndPort>();
	    //Jedis Cluster will attempt to discover cluster nodes automatically
		for(int i = 0; i < hosts.length; i++) {
			String [] datas = hosts[i].split(":");
			jedisClusterNodes.add(new HostAndPort(datas[0],Integer.parseInt(datas[1])));
		}
		jedisCluster = new JedisCluster(jedisClusterNodes,5000, 1000, 3,"kjzd@Admin", jedisPoolConfig);
	    
	}
}
