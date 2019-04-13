package redis.provide;


import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;
import redis.clients.jedis.JedisPoolConfig;
import util.StringUtils;

@PropertiesFilePath("/redis.properties")
public class JedisPoolConfigFactory extends ApplicationPropertiesBase {

	public static JedisPoolConfigFactory factory = new JedisPoolConfigFactory();

	@PropertiesName("redis.hosts")
	public String pro_hosts ;
	@PropertiesName("redis.maxTotal")
	public String maxTotal ;
	@PropertiesName("redis.maxIdle")
	public String maxIdle ;
	@PropertiesName("redis.minEvictableIdleTimeMilli")
	public String minEvict;
	@PropertiesName("redis.testOnBorrow")
	public String testOnBorrow;
	@PropertiesName("redis.timeBetweenEvictionRunsMillis")
	public String timeBERM;
	@PropertiesName("redis.maxWaitMillis")
	public String maxWaitMillis;

	static String[] hosts;
	static JedisPoolConfig poolConfig;

	@Override
	protected void initialization() {
		super.initialization();
		hosts = StringUtils.trim(pro_hosts).split(",");
		poolConfig = new JedisPoolConfig();
		//最大连接数，默认8
		poolConfig.setMaxTotal(Integer.parseInt(StringUtils.trim(maxTotal)));
		//最大空闲数,默认8
		poolConfig.setMaxIdle(Integer.parseInt(StringUtils.trim(maxIdle)));
		//最小空闲连接数，默认0
		poolConfig.setMinIdle(3);
		//对象最小空闲时间，默认1800000毫秒(30分钟)
		poolConfig.setMinEvictableIdleTimeMillis(Integer.parseInt(StringUtils.trim(minEvict)));
		//获取连接的最大等待毫秒数。如果设为小于0，则永远等待
		poolConfig.setMaxWaitMillis(!StringUtils.isEmpty(maxWaitMillis) ? Integer.parseInt(StringUtils.trim(maxWaitMillis)) : -1);

		//在创建对象时检测对象是否有效，true是，默认值是false
		poolConfig.setTestOnCreate(true);
		//从对象池获取对象时检测对象是否有效，默认false
		poolConfig.setTestOnBorrow(StringUtils.trim(testOnBorrow) == "true"?true:false);
		//在向对象池中归还对象时是否检测对象有效，true是，默认值是false
		poolConfig.setTestOnReturn(true);
		//在检测空闲对象线程检测到对象不需要移除时，是否检测对象的有效性。true是，默认值是false
		poolConfig.setTestWhileIdle(true);
		//检测空闲对象线程每次检测的空闲对象的数量。默认值是3；如果这个值小于0,则每次检测的空闲对象数量等于当前空闲对象数量除以这个值的绝对值，并对结果向上取整
		poolConfig.setNumTestsPerEvictionRun(3);
		//是否启用后进先出, 默认true
		poolConfig.setLifo(true);
		//多长时候执行一次空闲对象检测。单位是毫秒数。如果小于等于0，则不执行检测线程。默认值是-1
		poolConfig.setTimeBetweenEvictionRunsMillis(Integer.parseInt(StringUtils.trim(timeBERM)));
		//当对象池没有空闲对象时，新的获取对象的请求是否阻塞。true阻塞。默认值是true;
		poolConfig.setBlockWhenExhausted(true);

		///是否启用pool的jmx管理功能, 默认true
		poolConfig.setJmxEnabled(true);
	}

	static {

	}
	
	public static JedisPoolConfig getConfig() {
		return poolConfig;
	}

	public static String[] getHosts() {
		return hosts;
	}

	public static void setHosts(String[] hosts) {
		JedisPoolConfigFactory.hosts = hosts;
	}
	
}
