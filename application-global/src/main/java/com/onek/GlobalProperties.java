package com.onek;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @Author: leeping
 * @Date: 2019/3/20 10:56
 */
@PropertiesFilePath("/global.properties")
public class GlobalProperties extends ApplicationPropertiesBase {
    public static GlobalProperties INSTANCE = new GlobalProperties();

    @PropertiesName("file.server.address")
    public String fileServerAddress;





}
