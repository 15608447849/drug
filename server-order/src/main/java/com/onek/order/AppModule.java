package com.onek.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.onek.context.AppContext;
import com.onek.entity.ShoppingCartDTO;
import com.onek.entitys.Result;
import com.onek.util.IceRemoteUtil;
import com.onek.util.prod.ProdEntity;
import util.GsonUtils;

import java.util.HashMap;

import static com.onek.order.ShoppingCartModule.queryShopCartNumBySku;

/**
 * @Author: leeping
 * @Date: 2019/6/14 19:40
 */
public class AppModule {


    private static class BeanGop{
        long sku;//商品唯一标识
        int number;//加入采购车数量
        String pkgno = "0";//套餐码
    }
    /**
     * 对一件商品进行 + - 操作
     * sku
     * number -1 减单位数 , -2 加单位数 , >0 手动输入
     * 返回 购物车结果
     */
    public Result addCartOption(AppContext context){

        int compid = context.getUserSession().compId;

        BeanGop beanGop = GsonUtils.jsonToJavaBean(context.param.json,BeanGop.class);


        if (beanGop == null) return new Result().fail("数据不正确",0);

        //判断加入购物车的商品为套餐
        if(beanGop.sku<=0 && Long.parseLong(beanGop.pkgno)>0){
            int pkgnum = 0; //购物车数量
            String arr = ShoppingCartModule.queryPkgShopCartNum(compid,beanGop.pkgno);
            context.logger.print("当前购物车数量参数 = " + arr);
            if (arr != null && !arr.isEmpty()) {
                JSONArray goodsArr = JSON.parseArray(arr);
                int pkgNo = goodsArr.getJSONObject(0).getInteger("pkgno");
                int num = goodsArr.getJSONObject(0).getInteger("pnum");
                long sku = goodsArr.getJSONObject(0).getLong("sku");
                ProdEntity info = IceRemoteUtil.getProdBySku(sku);
                if (info == null) return new Result().fail("当前购物车套餐数量为0",0);

                //获取到当前购物车套餐数量
                pkgnum = num/info.getMedpacknum();
            }
            context.logger.print("当前购物车套餐数量 = " + pkgnum);
            if(beanGop.number == -1 && pkgnum<=0){
                return new Result().fail("套餐数量不能再减少了",0);
            }
            if(beanGop.number == -2){//加
                beanGop.number = 1;
            }else if(beanGop.number == -1){ //减
                beanGop.number = -1;
            }else{//手动输入
                beanGop.number = beanGop.number - pkgnum;
            }
            if(beanGop.number == 0 ){
                return new Result().success("加入购物车成功","调用成功");
            }
            context.logger.print("最终添加购物车套餐数量 = " + beanGop.number);
            Result res =  writePkgToShopCat(compid,beanGop.pkgno,beanGop.number,context);


            int updateNum = beanGop.number+pkgnum;

            context.logger.print("购物车中存在的套餐数量：=="+updateNum);
            if(res.code == 200){
                if(updateNum<=0)
                    res =  removePkgToShopCat(compid,beanGop.pkgno,"",context).success("购物车更新成功","调用成功");
            }
            return res;
        }

        //获取商品的购物车数量
        int current = queryShopCartNumBySku(compid,beanGop.sku);

        context.logger.print("当前购物车数量 = " + current);

        //获取商品信息
        ProdEntity info = IceRemoteUtil.getProdBySku(beanGop.sku);
        if (info == null) return new Result().fail("没有商品信息",0);

        //获取单位数量
        int unit = info.getMedpacknum();

        if (beanGop.number == -1) {
            //减
            if (current >= unit){
                beanGop.number = -unit;
            }else{
                beanGop.number = -current;
            }
        }else if (beanGop.number == -2){
            //加
            beanGop.number = unit;
        }else{
           //输入的情况
           if (beanGop.number >= 0 && (beanGop.number % unit) != 0) return new Result().fail("请购买中包装("+unit+")的整数倍",current);
           //差值
            beanGop.number = beanGop.number - current;
        }
        context.logger.print(" 增量值 " + beanGop.number);

        int temp = current + beanGop.number;

        int limit = ShoppingCartModule.getCanbuySkuNum(compid,beanGop.sku,temp,0);

        context.logger.print("limit = " + limit+" , unit = "+ unit +" , cur =" + current +" , 请求购买总数量: " + temp);

        if (temp > limit) return new Result().fail("超过商品限制数",current);

        //写入商品购物车数量
        Result result = writeNumberBySku(compid,beanGop.number,beanGop.sku,context);

        boolean flag =   result.isSuccess();

        //获取商品的购物车数量
        current = queryShopCartNumBySku(compid,beanGop.sku);


        if(flag){
            if(temp<=0)
                flag = removePkgToShopCat(compid,"",String.valueOf(beanGop.sku),context).isSuccess();
        }
        context.logger.print("最终结果数量: " + temp);

        return flag ? new Result().success("已加入购物车",current) : new Result().fail(result.message,current);
    }

    //写入商品数量到购物车
    private static Result writeNumberBySku(int compid, int number, long sku,AppContext context) {
        HashMap map = new HashMap();
        map.put("pdno",sku);
        map.put("pnum",number);
        map.put("compid",compid);
        map.put("checked",-2);
        context.param.json = GsonUtils.javaBeanToJson(map);
        return new ShoppingCartModule().saveShopCart(context);

    }

    //添加套餐至购物车
    private static Result writePkgToShopCat(int compid,String pkgno,int pkgnum,AppContext appContext){
        HashMap map = new HashMap();
        map.put("pkgno",pkgno);
        map.put("pkgnum",pkgnum);
        map.put("compid",compid);
        map.put("checked",1);
        appContext.param.json = GsonUtils.javaBeanToJson(map);
        return new ShoppingCartModule().saveShopCart(appContext);
    }


    //当购物车套餐数量为时移除当前套餐

    private static Result removePkgToShopCat(int compid,String pkgno,String ids,AppContext appContext){
        HashMap map = new HashMap();
        map.put("pkgids",pkgno);
        map.put("ids",ids);
        map.put("compid",compid);
        appContext.param.json = GsonUtils.javaBeanToJson(map);
        return new ShoppingCartModule().clearShopCart(appContext);
    }
}
