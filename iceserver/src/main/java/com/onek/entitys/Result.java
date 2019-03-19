package com.onek.entitys;


import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;

import java.util.HashMap;

public class Result {



   interface CODE{
      int FAIL = -1;
      int INTERCEPT = -2;
      int SUCCESS = 200;
   }

   interface MESSAGE{
      String FAIL = "调用失败";
      String SUCCESS = "调用成功";
      String INTERCEPT = "拒绝访问";
   }

   public int code = CODE.FAIL;
   public String message = MESSAGE.FAIL;

   public Object data ;

   public HashMap<String,Object> map;

   private Integer pageNo;

   private Integer pageSize;

   private Integer total;

   public Result message(String message){
      this.message = message;
      return this;
   }

   public Result fail(String message){
      this.code = CODE.FAIL;
      this.message = message;
      return this;
   }

   public Result fail(String message,Object data){
      this.code = CODE.FAIL;
      this.message = message;
      this.data = data;
      return this;
   }



   public Result setHashMap(String key,Object value){
      if (map == null) map = new HashMap<>();
      map.put(key,value);
      return this;
   }

   public Result success(String message,Object data) {
      this.code = CODE.SUCCESS;
      this.message = message;
      this.data = data;
      return this;
   }

   public Result success(Object data) {
      this.code = CODE.SUCCESS;
      this.message = MESSAGE.SUCCESS;
      this.data = data;
      return this;
   }

   public Result intercept(Object data){
      this.code = CODE.INTERCEPT;
      this.message = MESSAGE.INTERCEPT;
      this.data = data;
      return this;
   }



   public Result setQuery(Object data, PageHolder pageHolder) {
      success(data);

      if (pageHolder != null && pageHolder.value != null) {
         Page page = pageHolder.value;

         this.pageNo = page.pageIndex;
         this.pageSize = page.pageSize;
         this.total = page.totalItems;
      }

      return this;
   }

}
