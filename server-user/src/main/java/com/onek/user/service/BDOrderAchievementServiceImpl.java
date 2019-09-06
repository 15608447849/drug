package com.onek.user.service;

import com.alibaba.fastjson.JSONObject;
import com.onek.user.entity.BDCompVO;
import com.onek.user.entity.BDToOrderAchieveemntVO;
import com.onek.user.operations.BDAchievementOP;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BDOrderAchievementServiceImpl {

    private static final int _APPROVAL = 256;
    private static final int _NOTAPPROVAL = 128;


    public static String excall(long uid, List<BDCompVO> compList, List<BDToOrderAchieveemntVO> oList, Map bdsum, Map bdNewAddSum ) {


        JSONObject jsonObject = new JSONObject();

        getOrderInfoByCus(jsonObject,compList,oList, uid ,bdsum, bdNewAddSum );
        //System.out.println(jsonObject.toString());
        return jsonObject.toString();

    }

    /**
     * 获取每个门店信息
     * @param jsonObject
     * @param oList
     * @param uid
     */
    private static void getOrderInfoByCus(JSONObject jsonObject,List<BDCompVO> compList, List<BDToOrderAchieveemntVO> oList,long uid, Map bdsum, Map bdNewAddSum ) {
        List<BDCompVO> list = getCompInfo(compList, uid);
        jsonObject.put("ccustruenum",getCustruenum(list,_APPROVAL)); //审核通过门店
        jsonObject.put("ccusfalsenum",getCustruenum(list,0)-getCustruenum(list,_APPROVAL));//审核未通过
        jsonObject.put("cregnum",getCustruenum(list,0));//总共注册门店
        if(bdsum==null){
            jsonObject.put("cumulticeSum",0); //累计首购数
        }else{
            jsonObject.put("cumulticeSum",bdsum.get(uid)); //累计首购数
        }
        if(bdNewAddSum == null){
            jsonObject.put("cumulticeNewAdd", 0);//新增首购数
        }else {
            jsonObject.put("cumulticeNewAdd", bdNewAddSum.get(uid));//新增首购数
        }
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
            jsonObject.put("realrefamt", "0"); //退款金额
            jsonObject.put("subsidyamt", "0"); //补贴金额
            jsonObject.put("amtsum", "0"); //GMV小计金额
            jsonObject.put("maxpayamt", "0"); //最高支付金额
            jsonObject.put("minpayamt", "0"); //最低支付金额
            jsonObject.put("avgpayamt", "0"); //平均支付金额
        }else {
            jsonObject.put("ocancelord", order.getCanclord()); //交易取消订单
            jsonObject.put("ocompleteord", order.getCompleteord()); //订单交易完成数
            jsonObject.put("oreturnord", order.getReturnord()); //退货订单数
            jsonObject.put("oafsaleord", order.getAfsaleord()); //售后订单数
            jsonObject.put("osumord", getOrderNum( Integer.parseInt(order.getCanclord()),Integer.parseInt(order.getCompleteord())));//,Integer.parseInt(order.getReturnord()),Integer.parseInt(order.getAfsaleord()))); //小计
//            jsonObject.put("oreturnrate", getRate(jsonObject.getString("oreturnord"),jsonObject.getString("osumord"))); //退货率
//            jsonObject.put("ofsalerate", getRate(jsonObject.getString("oafsaleord"),jsonObject.getString("osumord"))); //售后率
            jsonObject.put("canclordamt", getDoubleValue(new BigDecimal(order.getCanclordamt()))); //取消订单金额
            jsonObject.put("originalprice",getDoubleValue(new BigDecimal(order.getOriginalprice()))); //原价交易金额
            jsonObject.put("payamt", getDoubleValue(new BigDecimal(order.getPayamt()))); //实付交易额
            jsonObject.put("realrefamt", getDoubleValue(new BigDecimal(order.getRealrefamt()))); //退款金额
            jsonObject.put("subsidyamt", getDoubleValue(new BigDecimal(getSubsidyAmtSum(order.getOriginalprice(),order.getPayamt())))); //补贴金额
            jsonObject.put("amtsum", getDoubleValue(new BigDecimal(getAmtSum(order.getCanclordamt(),order.getOriginalprice())))); //GMV小计金额
            jsonObject.put("maxpayamt", getDoubleValue(order.getMaxpayamt())); //最高支付金额
            jsonObject.put("minpayamt", getDoubleValue(order.getMinpayamt())); //最低支付金额
            jsonObject.put("avgpayamt", getDoubleValue(new BigDecimal(order.getAvgpayamt()))); //平均支付金额
        }
    }

    /**
     * 相除
     * @param num
     * @return
     */
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

    /**
     * 相加
     * @param canclordamt
     * @param originalprice
     * @return
     */
    private static double getAmtSum(String canclordamt,String originalprice){
        BigDecimal canclordamts = new BigDecimal(canclordamt);
        BigDecimal originalprices = new BigDecimal(originalprice);

        return canclordamts.add(originalprices).doubleValue();
    }

    /**
     * 相减
     * @param
     * @return
     */
    private static double getSubsidyAmtSum(String originalprice,String payamt){
        BigDecimal originalprices = new BigDecimal(originalprice);
        BigDecimal payamts = new BigDecimal(payamt);

        return originalprices.subtract(payamts).doubleValue();
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
    private static int getCustruenum(List<BDCompVO> boList, int status) {
        int result =0;
        List gl = new ArrayList();
        for (int i = 0; i < boList.size(); i++) {
            BDCompVO comp = boList.get(i);
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
    private static List<BDCompVO> getCompInfo(List<BDCompVO> boList, long uid){
        List<BDCompVO> list = new ArrayList<BDCompVO>();
        for (int i = 0; i < boList.size(); i++) {
            BDCompVO comp = boList.get(i);
            if(String.valueOf(comp.getInviter()).equals(String.valueOf(uid))) {
                list.add(comp);
            }
        }
        return list;
    }

}
