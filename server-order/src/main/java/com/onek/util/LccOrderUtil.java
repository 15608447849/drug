package com.onek.util;

import com.hsf.framework.api.cstruct.UserParam;
import com.hsf.framework.order.OrderICE;
import com.hsf.framework.order.OrderServicePrx;
import com.onek.entity.TranOrder;

public class LccOrderUtil {

    public static void addLccOrder(TranOrder tranOrder,String consignee,String consphone,String arriaddr) {

        OrderICE orderIce = new OrderICE();

        orderIce.pubercompid = tranOrder.getBusno()+"";
        orderIce.puberid = "";

        orderIce.phone1 = "";
        orderIce.phone2 = "";
        orderIce.billno = tranOrder.getOrderno();

        orderIce.startc = ""; // todo
        orderIce.startaddr = ""; // todo
        orderIce.arriarc = tranOrder.getRvaddno() + "";
        orderIce.arriaddr = arriaddr;

        orderIce.wm = 0.1;
        orderIce.wmdictc = "43"; // 立方
        orderIce.num = tranOrder.getPdnum();
        orderIce.numdictc = "51"; //件
        orderIce.padictc = "61";
        orderIce.ctdictc = "32"; // 药品

        orderIce.vnum = 1;
        orderIce.vldictc = "1";
        orderIce.vtdictc = "11";
        orderIce.tndictarr = new int[] { 71 };
        orderIce.price = tranOrder.getFreight();
        orderIce.codamt = 0;

        orderIce.pmdictc = "84";
        orderIce.insureamt = 0;

        orderIce.consignee = consignee;
        orderIce.consphone = consphone;
        orderIce.ptdictc = "22"; // 线下到付
        orderIce.dmdictc = "93"; // 送货不上楼
        orderIce.redictc = "101"; // 无回单
        orderIce.priority = 0;

        OrderServicePrx orderServicePrx = (OrderServicePrx) RpcClientUtil
                .getServicePrx(OrderServicePrx.class);
        orderIce.orderno = orderServicePrx.generateOrderNo();

        UserParam param = new UserParam();
        param.uid = orderIce.puberid;
        param.compid = orderIce.pubercompid;

        String result = orderServicePrx.addOrder(param, orderIce);
        System.out.println(result);

        UserParam robbParam = new UserParam();
        robbParam.uid = ""; // Todo
        robbParam.compid = ""; // Todo
        orderServicePrx.robbingOrder(robbParam, new String[] {orderIce.orderno, orderIce.pubercompid});


    }
}
