package com.onek.user.service;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @Author: leeping
 * @Date: 2019/3/20 11:17
 */
@PropertiesFilePath("/server_user.properties")
public class USProperties extends ApplicationPropertiesBase {
    public static USProperties INSTANCE = new USProperties();

    @PropertiesName("verification.code.image.survive.time")
    public int vciSurviveTime;

    @PropertiesName("verification.code.sms.survive.time")
    public int smsSurviveTime;

    @PropertiesName("store.login.fail.num.min")
    public int sLoginNumMin;

    @PropertiesName("store.login.fail.num.max")
    public int sLoginNumMax;

    @PropertiesName("store.login.fail.lock.time")
    public int sLoginLockTime;


}
