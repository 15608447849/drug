package com.onek.server.infimp;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/10 10:37
 */
public interface IPushMessageStore {

    long storeMessageToDb(String identityName, String message) ;
    void changeMessageStateToDb(String identityName,long id) ;
    List<String> checkOfflineMessageFromDbByIdentityName(String identityName);

}
