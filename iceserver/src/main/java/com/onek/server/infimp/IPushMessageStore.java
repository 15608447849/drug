package com.onek.server.infimp;

import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/4/10 10:37
 */
public interface IPushMessageStore {

   class IPMessage{
        public String identityName;
        public String content;
        public long id;

       public IPMessage(String identityName,  String message) {
           this.identityName = identityName;
           this.content = message;
       }
       public IPMessage(String identityName, long id, String message) {
           this.identityName = identityName;
           this.id = id;
           this.content = message;
       }

       @Override
       public String toString() {
           return "IPMessage{" +
                   "identityName='" + identityName + '\'' +
                   ", content='" + content + '\'' +
                   ", id=" + id +
                   '}';
       }
   }


    long storeMessageToDb(IPMessage message) ;
    void changeMessageStateToDb(IPMessage message) ;
    List<IPMessage> checkOfflineMessageFromDbByIdentityName(String identityName);
    String convertMessage(IPMessage message);

}
