package com.onek.entitys;


import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;

public class Result {

   interface CODE{
      int FAIL = -1;
      int SUCCESS = 200;
   }

   interface MESSAGE{
      String FAIL = "fail";
      String SUCCESS = "success";
   }

   public int code = CODE.FAIL;
   public String message = MESSAGE.FAIL;

   public Object data ;

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

   public Result success(Object data) {
      this.code = CODE.SUCCESS;
      this.message = MESSAGE.SUCCESS;
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
