package com.onek.order;

import com.onek.context.AppContext;
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
        int pkgnum; //套餐数量
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
            if(beanGop.pkgnum<=0){
                return new Result().fail("套餐添加数量不能小于0",0);
            }
            return writePkgToShopCat(compid,beanGop.pkgno,beanGop.pkgnum,context);
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
        appContext.param.json = GsonUtils.javaBeanToJson(map);
        return new ShoppingCartModule().saveShopCart(appContext);
    }
}
