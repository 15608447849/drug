package com.onek.global.sms;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @program: framework
 * @description: 属性文件帮助类
 * @author: liu yan
 * @create: 2018-07-12 18:20
 */

@PropertiesFilePath("/smsapplication.properties")
public class SmsProperties extends ApplicationPropertiesBase {
    private static SmsProperties smsProperties = new SmsProperties();

    public static SmsProperties getInstance() {
        return smsProperties;
    }

    private SmsProperties() {
    }

    @PropertiesName("token.expireSecond")
    private String tokenExpireSecond;
    @PropertiesName("sms.expireSecond")
    private String smsExpireSecond;
    @PropertiesName("sms.ip")
    private String smsIp;
    @PropertiesName("sms.port")
    private String smsPort;
    @PropertiesName("sms.username")
    private String smsUserName;
    @PropertiesName("sms.password")
    private String smsPassword;
    private String orderConfimTemplate;

    public int getTokenExpireSecond() {
        return Integer.valueOf(tokenExpireSecond);
    }

    public String getSmsIp() {
        return smsIp;
    }

    public String getSmsPort() {
        return smsPort;
    }

    public String getSmsUserName() {
        return smsUserName;
    }

    public int getSmsExpireSecond() {
        return Integer.valueOf(smsExpireSecond);
    }

    public String getSmsPassword() {
        return smsPassword;
    }
    
    public String getOrderConfimTemplate() {
		return orderConfimTemplate;
	}
}