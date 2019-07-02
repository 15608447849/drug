package com.onek.prop;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @Author: leeping
 * @Date: 2019/3/20 10:56
 */
@PropertiesFilePath("/app.properties")
public class AppProperties extends ApplicationPropertiesBase {
    public static AppProperties INSTANCE = new AppProperties();

    @PropertiesName("file.server.address")
    public String fileServerAddress;

    @PropertiesName("ice.grid.name")
    public String iceInstance;

    @PropertiesName("ice.master.info")
    public String iceServers;

    @PropertiesName("ice.call.timeout")
    public int iceTimeOut;

    @PropertiesName("pay.server.url")
    public String payUrlPrev;

    @PropertiesName("erp.server.url")
    public String erpUrlPrev;





}
