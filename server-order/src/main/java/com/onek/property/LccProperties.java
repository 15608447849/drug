package com.onek.property;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

@PropertiesFilePath("/oneklogistics.properties")
public class LccProperties extends ApplicationPropertiesBase {

    public static LccProperties INSTANCE = new LccProperties();

    @PropertiesName("order.pubercompid")
    public int pubercompid;

    @PropertiesName("order.puberid")
    public int puberid;

    @PropertiesName("order.startc")
    public int startc;

    @PropertiesName("order.startaddr")
    public String startaddr;

    @PropertiesName("order.robbid")
    public int robbid;

    @PropertiesName("order.robbcompid")
    public int robbcompid;
}
