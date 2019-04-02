package com.onek.prop;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

/**
 * @Author: leeping
 * @Date: 2019/3/20 10:56
 */
@PropertiesFilePath("/fileserver.properties")
public class FileServerProperties extends ApplicationPropertiesBase {
    public static FileServerProperties INSTANCE = new FileServerProperties();

    @PropertiesName("file.server.address")
    public String fileServerAddress;
    @PropertiesName("file.default.directory")
    public String fileDefaultDir;





}
