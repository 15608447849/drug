package com.onek.entitys;


import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;

import java.util.HashMap;

public class Result {
    interface CODE{
      int FAIL = -1;
      int ERROR = 0;
      int SUCCESS = 200;
   }

   interface MESSAGE{
      String FAIL = "调用失败";
      String SUCCESS = "调用成功";
      String ERROR = "异常捕获";
   }

   public int code = CODE.FAIL;
   public String message = MESSAGE.FAIL;

   public Object data ;

   private HashMap<String,Object> map;

   private Integer pageNo;

   private Integer pageSize;

   private Integer total;

   public boolean isSuccess(){
      return code == CODE.SUCCESS;
   }

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

   public Result success() {
      this.code = CODE.SUCCESS;
      this.message = MESSAGE.SUCCESS;
      return this;
   }

   public Result success(Object data) {
      this.code = CODE.SUCCESS;
      this.message = MESSAGE.SUCCESS;
      this.data = data;
      return this;
   }

   public Result intercept(int code,String cause){
      this.code = code;
      this.message = cause;
      return this;
   }


   public Result error(String msg,Throwable e) {
      this.code = CODE.ERROR;
      this.message = MESSAGE.ERROR;
      this.data = msg  +" , " +e;
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
