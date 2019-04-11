[["java:package:com.onek.server"]]
#pragma once
#include <C:/Ice-3.6.3/slice/Ice/Identity.ice>
/**
ice 接口调用
*/
module inf{

    /** string 数组 */
   sequence<string> stringArray;

   /** 字节数组 */
   sequence<byte> byteArray;

    /** 方法参数 */
      struct IParam{
          string json;
          stringArray arrays;
          byteArray bytes;
          int pageIndex;
          int pageNumber;
          string extend;
          string token;
      };

      /** 接口调用结构体 */
      struct IRequest{
        string pkg;
        string cls;
        string method;
        IParam param;
      };

      /** 服务接口 interface */
      interface Interfaces{
          /** 前后台交互 */
          string accessService(IRequest request);

          /** 消息推送-服务端 / 客户端上线  */
          void online(Ice::Identity identity);
          /** 消息推送-服务端 / 客户端下线 */
          void offline(string identityName);
          /** 消息推送-服务端 / 后端服务调用 - 向指定客户端发送消息 */
          void sendMessageToClient(string identityName,string message);
      };


    /** 消息推送-客户端 需要具体客户端实现 */
    interface PushMessageClient{
        /** 客户端接受服务端 消息 */
        void receive(string message);
    };

};
