import com.onek.client.IceClient;
import redis.clients.jedis.Client;
import util.GsonUtils;

import java.util.HashMap;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:49
 */
public class client_new {
    private static  IceClient ic;
    public static void main(String[] args) {
        String host = "114.116.149.145";

        ic = new IceClient("DemoIceGrid",host,4061);

        ic.startCommunication();
        getImageCode();
        ic.stopCommunication();
    }

    private static void getImageCode() {
        HashMap hashMap = new HashMap();
        hashMap.put("type",1);
        String result = ic.settingProxy("userServer")
                .settingReq("/*/*@@##*/*/","LoginRegistrationModule","obtainVerificationCode")
                .settingParam(GsonUtils.javaBeanToJson(hashMap)).executeSync();
        System.out.println(result);
    }

}
