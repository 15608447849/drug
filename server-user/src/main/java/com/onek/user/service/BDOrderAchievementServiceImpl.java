package com.onek.user.service;

import com.alibaba.fastjson.JSONObject;
import com.onek.user.entity.BDToOrderAchieveemntVO;
import com.onek.user.operations.BDAchievementOP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BDOrderAchievementServiceImpl {

    private static final int _APPROVAL = 256;
    private static final int _NOTAPPROVAL = 128;


    public static String excall(long uid, List<BDAchievementOP.Comp> compList, List<BDToOrderAchieveemntVO> oList) {


        JSONObject jsonObject = new JSONObject();

        getOrderInfoByCus(jsonObject,compList,oList, uid);
        //System.out.println(jsonObject.toString());
        return jsonObject.toString();

    }

    /**
     * 获取每个门店信息
     * @param jsonObject
     * @param oList
     * @param uid
     */
    private static void getOrderInfoByCus(JSONObject jsonObject,List<BDAchievementOP.Comp> compList, List<BDToOrderAchieveemntVO> oList,long uid) {
        List<BDAchievementOP.Comp> list = getCompInfo(compList, uid);
        jsonObject.put("ccustruenum",getCustruenum(list,_APPROVAL)); //审核通过门店
        jsonObject.put("ccusfalsenum",getCustruenum(list,0)-getCustruenum(list,_APPROVAL));//审核未通过
        jsonObject.put("cregnum",getCustruenum(list,0));//总共注册门店

        BDToOrderAchieveemntVO order = getOrderInfo(oList,uid);
        if(order == null) {
            jsonObject.put("ocancelord", "0"); //交易取消订单
            jsonObject.put("ocompleteord", "0"); //订单交易完成数
            jsonObject.put("oreturnord", "0"); //退货订单数
            jsonObject.put("oafsaleord", "0"); //售后订单数
//            jsonObject.put("oreturnrate", "0"); //退货率
//            jsonObject.put("ofsalerate", "0"); //售后率
            jsonObject.put("osumord", "0"); //小计
            jsonObject.put("canclordamt", "0"); //取消订单金额
            jsonObject.put("originalprice","0"); //原价交易金额
            jsonObject.put("payamt", "0"); //实付交易额
            jsonObject.put("maxpayamt", "0"); //最高支付金额
            jsonObject.put("minpayamt", "0"); //最高支付金额
            jsonObject.put("avgpayamt", "0"); //最高支付金额
        }else {
            jsonObject.put("ocancelord", order.getCanclord().longValue()); //交易取消订单
            jsonObject.put("ocompleteord", order.getCompleteord().intValue()); //订单交易完成数
            jsonObject.put("oreturnord", order.getReturnord().intValue()); //退货订单数
            jsonObject.put("oafsaleord", order.getAfsaleord().intValue()); //售后订单数
            jsonObject.put("osumord", getOrderNum( order.getCanclord().intValue(),order.getCompleteord().intValue(),order.getReturnord().intValue(),order.getAfsaleord().intValue() )); //小计
//            jsonObject.put("oreturnrate", getRate(jsonObject.getString("oreturnord"),jsonObject.getString("osumord"))); //退货率
//            jsonObject.put("ofsalerate", getRate(jsonObject.getString("oafsaleord"),jsonObject.getString("osumord"))); //售后率
            jsonObject.put("canclordamt", getDoubleValue(order.getCanclordamt())); //取消订单金额
            jsonObject.put("originalprice",getDoubleValue(order.getOriginalprice())); //原价交易金额
            jsonObject.put("payamt", getDoubleValue(order.getPayamt())); //实付交易额
            jsonObject.put("maxpayamt", getDoubleValue(order.getMaxpayamt())); //最高支付金额
            jsonObject.put("minpayamt", getDoubleValue(order.getMinpayamt())); //最高支付金额
            jsonObject.put("avgpayamt", getDoubleValue(order.getAvgpayamt())); //最高支付金额
        }
    }

    private static double getDoubleValue(BigDecimal num) {
        return num.divide(new BigDecimal(String.valueOf(100)),2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
    private static double getDoubleValue(long num) {
        return new BigDecimal(num).divide(new BigDecimal(String.valueOf(100)),2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private static double getRate(String chd,String moter) {
        BigDecimal chrid = new BigDecimal(chd);
        BigDecimal mote = new BigDecimal(moter);

        return chrid.divide(mote, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static int getOrderNum(int ...ordnum) {
        int sum = 0;
        for (int i = 0; i < ordnum.length; i++) {
            sum += ordnum[i];
        }
        return sum;
    }
    /**
     * 获取当前BD门店订单数据
     * @param boList
     * @param uid
     * @return
     */
    private static BDToOrderAchieveemntVO getOrderInfo(List<BDToOrderAchieveemntVO> boList,long uid){
        BDToOrderAchieveemntVO ord = null;
        for (BDToOrderAchieveemntVO orderVO : boList) {
            if(String.valueOf(orderVO.getInviter()).equals(String.valueOf(uid)))
                ord = orderVO;
        }
        return ord;
    }

    /**
     * 获取审核通过门店数
     * status为0则查询所有注册门店
     * status为256则查询所有审核通过门店
     * status为128则查询所有未审核，未通过门店
     * @return
     */
    private static int getCustruenum(List<BDAchievementOP.Comp> boList, int status) {
        int result =0;
        List gl = new ArrayList();
        for (int i = 0; i < boList.size(); i++) {
            BDAchievementOP.Comp comp = boList.get(i);
            if(status>0) {
                if((comp.getCstatus()&status) >0) {
                    result++;
                    gl.add(comp.getCompid());
                }
            }else {
                result++;
                gl.add(comp.getCompid());
            }
        }
        return result;
    }


    /**
     * 获取BD下门店
     * @param boList
     * @param uid
     * @return
     */
    private static List<BDAchievementOP.Comp> getCompInfo(List<BDAchievementOP.Comp> boList, long uid){
        List<BDAchievementOP.Comp> list = new ArrayList<BDAchievementOP.Comp>();
        for (int i = 0; i < boList.size(); i++) {
            BDAchievementOP.Comp comp = boList.get(i);
            if(String.valueOf(comp.getInviter()).equals(String.valueOf(uid))) {
                list.add(comp);
            }
        }
        return list;
    }

}