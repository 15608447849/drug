package com.onek.iceabs;

import Ice.Logger;
import org.hyrdpf.util.LogUtil;

/**
 * @Author: leeping
 * @Date: 2019/4/3 10:57
 */
public class IceLog4jLogger implements Ice.Logger {

    private final org.apache.logging.log4j.Logger logger;

    private final String prefix;

    public IceLog4jLogger(String prefix){
        LogUtil.setDefaultLoggerName(prefix);
        this.logger = LogUtil.getDefaultLogger();
        this.prefix = prefix;
    }

    @Override
    public Logger cloneWithPrefix(String prefix) {
        return new IceLog4jLogger(prefix);
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
        logger.trace(category,message);
    }

    @Override
    public void warning(String message) {
        logger.debug(message);
    }
}
