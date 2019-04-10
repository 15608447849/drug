package com.onek.prop;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:06
 */
@PropertiesFilePath("/iceinfo.properties")
public class IceMasterInfoProperties  extends ApplicationPropertiesBase {
    public static IceMasterInfoProperties INSTANCE = new IceMasterInfoProperties();
    @PropertiesName("ice.master.host")
    public String host;
    @PropertiesName("ice.master.port")
    public int port;
    @PropertiesName("ice.grid.name")
    public String name;

}
