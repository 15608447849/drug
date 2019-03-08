[["java:package:com.onek.server"]]
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
          string accessService(IRequest request);
      };

};
