package global;

import Ice.Application;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;
import com.onek.prop.IceMasterInfoProperties;
import com.onek.util.prod.ProdEntity;
import util.GsonUtils;
import util.StringUtils;

import java.util.HashMap;

/**
 * @Author: leeping
 * @Date: 2019/4/10 19:08
 * 远程调用工具
 */
public class IceRemoteUtil {
    private static IceClient ic ;
    static {
        ic = new IceClient(
                IceMasterInfoProperties.INSTANCE.name,
                IceMasterInfoProperties.INSTANCE.host,
                IceMasterInfoProperties.INSTANCE.port);
      ic.startCommunication();
    }

    public static String getProduceName(String pclass) {
        try {

            String result = ic.settingProxy("globalServer")
                    .settingReq("","CommonModule","getProduceName")
                    .settingParam(new String[]{pclass}).executeSync();
            System.out.println(result);
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            Object data = hashMap.get("data");
            if (data!=null) {
                return data.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ProdEntity getProdBySku(long sku) {
        try {
            String result = ic.settingProxy("globalServer")
                    .settingReq("","CommonModule","getProdBySku")
                    .settingParam(new String[]{sku+""}).executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            String json = hashMap.get("data").toString();
            ProdEntity prodEntity = GsonUtils.jsonToJavaBean(json,ProdEntity.class);
            return prodEntity;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param args 0短信模板序列id ,1及以后:模板需要的占位符信息参数
     */
    public static String getMessageByNo(String... args){
        try {
            String result = ic.settingProxy("globalServer")
                    .settingReq("","MessageModule","convertMessage")
                    .settingParam(args).executeSync();
            HashMap<String,Object> hashMap = GsonUtils.jsonToJavaBean(result,new TypeToken<HashMap<String,Object>>(){}.getType());
            String message = hashMap.get("data").toString();
           if (!StringUtils.isEmpty(message)) return message;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



}
