package com.onek.util;

import com.hsf.framework.api.cstruct.UserParam;
import com.hsf.framework.order.OrderICE;
import com.hsf.framework.order.OrderServicePrx;
import com.onek.entity.TranOrder;
import com.onek.property.LccProperties;
import org.hyrdpf.ds.AppConfig;

public class LccOrderUtil {

    public static String addLccOrder(TranOrder tranOrder,String consignee,String consphone,String arriaddr) {

        OrderICE orderIce = new OrderICE();

        orderIce.pubercompid = LccProperties.INSTANCE.pubercompid + "";
        orderIce.puberid = LccProperties.INSTANCE.puberid + "";

        orderIce.phone1 = "";
        orderIce.phone2 = "";
        orderIce.billno = tranOrder.getOrderno();

        orderIce.startc = LccProperties.INSTANCE.startc + "";
        orderIce.startaddr = LccProperties.INSTANCE.startaddr;
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
        robbParam.uid = LccProperties.INSTANCE.robbid + "";
        robbParam.compid = LccProperties.INSTANCE.robbcompid + "";
        result = orderServicePrx.robbingOrder(robbParam, new String[] {orderIce.orderno, orderIce.pubercompid});

        System.out.println(result);

        return result;

    }

    static {
        AppConfig.initLogger("log4j2.xml");
        AppConfig.initialize();
    }

    public static void main(String[] args) {
        TranOrder tranOrder = new TranOrder();
        tranOrder.setFreight(20);
        tranOrder.setRvaddno(10130803);
        tranOrder.setPdnum(2);
        tranOrder.setOrderno("1904180003102002");
        LccOrderUtil.addLccOrder(tranOrder, "小李", "14598763465","北京市天安门");
    }
}
