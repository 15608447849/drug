package global;

import Ice.Logger;
import org.hyrdpf.util.LogUtil;

/**
 * Copyright © 2018空间折叠【FOLDING SPACE】. All rights reserved.
 * @ClassName: CustomLoggerI
 * @Description: TODO 扩展ICE自带的日志记录器，采用Log4j2来输出日志。
 * @version: V1.0
 */
public class CustomLoggerI implements Logger {
	private final org.apache.logging.log4j.Logger logger;
	private final String prefix;
	
	public CustomLoggerI(String prefix){
		LogUtil.setDefaultLoggerName(prefix);
		this.logger = LogUtil.getDefaultLogger();
		this.prefix = prefix;
	}
	@Override
	public Logger cloneWithPrefix(String prefix) {
		return new CustomLoggerI(prefix);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override
	public void print(String message) {
		logger.info(message);
	}

	@Override
	public void trace(String category, String message) {
		logger.trace(category + " " + message);
	}

	@Override
	public void warning(String message) {
		logger.warn(message);
	}
}
