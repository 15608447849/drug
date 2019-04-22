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

    @PropertiesName("file.default.directory")
    public String fileDefaultDir;

    @PropertiesName("ice.master.host")
    public String masterHost;
    @PropertiesName("ice.master.port")
    public int masterPort;
    @PropertiesName("ice.grid.name")
    public String masterName;

    @PropertiesName("pay.server.url")
    public String payUrlPrev;





}
