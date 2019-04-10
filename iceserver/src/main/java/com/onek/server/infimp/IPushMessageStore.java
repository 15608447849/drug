package com.onek.server.infimp;

import java.util.List;
import java.util.Map;

/**
 * @Author: leeping
 * @Date: 2019/4/10 10:37
 */
public interface IPushMessageStore {

    long storeMessageToDb(String identityName, String message) ;
    void changeMessageStateToDb(String identityName,long id) ;
    Map<Long,String> checkOfflineMessageFromDbByIdentityName(String identityName);

}
