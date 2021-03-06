package com.onek.server.infimp;

import properties.abs.ApplicationPropertiesBase;
import properties.annotations.PropertiesFilePath;
import properties.annotations.PropertiesName;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@PropertiesFilePath("/iceapplication.properties")
public class IceProperties extends ApplicationPropertiesBase {

  public static IceProperties INSTANCE = new IceProperties();

  @PropertiesName("ice.push.message.allow.server.name")
  public String allowPushMessageServer;
  @PropertiesName("ice.server.package.map")
  public String pkgSrv ;
  @PropertiesName("ice.server.rep.group")
  public String repSrv ;
  @PropertiesName("ice.server.interceptor.list")
  public String intercept ;
  @PropertiesName("ice.app.context.imp")
  public String contextImp;
  @PropertiesName("ice.push.message.store.imp")
  public String pmStoreImp;

  //rep 列表 k = 服务名 v = 组名
  public HashMap<String,String> repSrvMap;

  //包反射列表  k = 服务名 , v = 路径
  public HashMap<String,String> pkgSrvMap;


  @Override
  protected void initialization() {
    super.initialization();
    pkgSrvMap = handlePropValue(pkgSrv);
    repSrvMap = handlePropValue(repSrv);
  }


  //切割
  private static HashMap<String,String> handlePropValue(String value){
    HashMap<String,String> map = new HashMap<>();
    try {
      String[] arr = value.split(";");
      for (String str : arr){
        String[] tarr = str.split(":");
        String v = tarr[0];
        String[] trarr = tarr[1].split(",");
        for (String k : trarr){
          map.put(k,v);
        }
      }
    } catch (Exception ignored) {
    }
    return map;
  }

  public List<String> getInterceptList() {
    String[] arr = new String[0];
    try {
      arr = intercept.split(";");
    } catch (Exception ignored) {
    }
    return Arrays.asList(arr);
  }



}
