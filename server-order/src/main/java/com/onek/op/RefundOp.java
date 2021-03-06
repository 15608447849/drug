package com.onek.op;

import Ice.Logger;
import com.onek.context.AppContext;
import com.onek.entitys.IOperation;
import com.onek.entitys.Result;
import com.onek.util.GenIdUtil;
import com.onek.util.IceRemoteUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static com.onek.util.FileServerUtils.refund;
import static constant.DSMConst.TD_TRAN_ASAPP;
import static constant.DSMConst.TD_TRAN_TRANS;

/**
 * @Author: leeping
 * @Date: 2019/7/18 14:39
 * 后台运营退款金额
 */
public class RefundOp implements IOperation<AppContext> {
    private static final String UPD_ASAPP_CK_FINISH_SQL =
            "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set ckstatus = ? " +
                    " where cstatus&1=0 and asno = ?  ";

    //更新订单售后状态
    private static final String UPD_ORDER_CK_SQL = "update {{?" + DSMConst.TD_TRAN_ORDER + "}} set ostatus=? "
            + " where cstatus&1=0 and orderno=? and ostatus = ? ";

    //更新订单相关商品售后状态
    private static final String UPD_ORDER_GOODS_CK_SQL = "update {{?" + DSMConst.TD_TRAN_GOODS + "}} set asstatus=? "
            + " where cstatus&1=0 and pdno=? and asstatus= ? and orderno=? and compid  = ? and pkgno = ?";

    //更新退款金额
    private static final String UPDATE_ORDER_REFAMT = "update {{?" + DSMConst.TD_TRAN_ASAPP + "}} set realrefamt = ? where cstatus&1=0 and asno = ?";

    private static List<Long> asnoList = new ArrayList<Long>();

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();
    long asno; //售后单号
    double refamt;//客户输入的退款金额
    double realrefamt; //财务实际输入的退款金额
    int astype;//售后类型
    Logger log;

    @Override
    public Result execute(AppContext context) {
        log = context.logger;
        log.print(asno+" , " + refamt+" , " + realrefamt+" , "+ astype);
        Result r = new Result().fail("退款失败");
        //判断售后类型 -> 1退款退货 2仅退款
        if (astype == 1 || astype == 2) {
            //判断退款金额
            if (realrefamt > refamt) {
                r.fail("实际退款金额大于用户请求退款的金额");
            }else{
                if (realrefamt <= 0){
                    r.fail("实际退款金额为0,无法退款");
                }else{
                    r = startRefund();
                }
            }
        }else{
            r.fail("售后类型不匹配");
        }
        //清空存储器
        asnoList.clear();
        return r;
    }

    private static class BackResult{
         int code = 0;
         Object data;
         String message;
    }


    private Result startRefund() {
        //判断售后订单是否在退款中
        if(asnoList.contains(asno)){
            return new Result().fail("订单正在退款中！");
        }else{
            asnoList.add(asno);
        }
        int year = Integer.parseInt("20" + String.valueOf(asno).substring(0, 2));
        //订单号,公司码,商品码sku
        String querySql = "SELECT orderno,compid,pdno FROM {{?" + TD_TRAN_ASAPP + "}} where asno = ? AND ckstatus = 1"; //通过售后订单, 查询售后状态 , 审核通过
        List<Object[]> lines = baseDao.queryNativeSharding(0, year, querySql, asno);
        if (lines.size() != 1) return new Result().fail("找不到有效的售后单");
        log.print("当前售后订单：" + asno + "准备退款");
        //公司码
        int compid = Integer.parseInt(lines.get(0)[1].toString());
        //订单号
        String orderNo = lines.get(0)[0].toString();

        //0 支付渠道 0-余额 1-微信 2-支付宝
        //1 交易金额
        //2 第三方交易号
        //3 支付来源
        //4 售后状态
        String selectSql = "SELECT payway, payprice, tppno, paysource,cstatus"
                + " FROM {{?" + TD_TRAN_TRANS + "}} "
                + " WHERE cstatus&1 = 0 AND orderno = ? AND paystatus = 1";

        lines = baseDao.queryNativeSharding(compid,
                year,
                selectSql, orderNo);


        if (lines.size() == 0) return new Result().fail("没有支付记录");

        //可退余额 , 可退线上支付金额 ,  如果是线上支付,线上支付的总金额
        BigDecimal bal = BigDecimal.ZERO;
        BigDecimal pay = BigDecimal.ZERO;
        BigDecimal payTotal = BigDecimal.ZERO;

        int onilePayType = -1;
        String otherNo = "";//第三方流水号
        int clientType = -1; //客户端类型

        for (Object[] rows : lines){
            int cstatus = StringUtils.checkObjectNull(rows[4],-1);
            int payway = StringUtils.checkObjectNull(rows[0],-1);
            BigDecimal payprice = BigDecimal.valueOf( StringUtils.checkObjectNull(rows[1],0));
                if (cstatus == -1) return new Result().fail("异常操作");
                if ( (cstatus & 1024) > 0){
                    //处理已退款记录
                    if (payway == 0) { //余额
                        bal = bal.subtract(payprice);
                    }else{//线上
                        pay = pay.subtract(payprice);
                    }
                }else{
                    //处理已付款记录
                    if (payway == 0) { //余额类型
                        bal = bal.add(payprice);
                    }else{//线上类型
                        pay = pay.add(payprice);
                        payTotal = payTotal.add(payprice);
                        onilePayType = payway;
                        otherNo = StringUtils.checkObjectNull(rows[2],"");
                        clientType = StringUtils.checkObjectNull(rows[3],-1);
                    }
                }
        }
        double _bal = bal.divide(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue();
        double _pay = pay.divide(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue();
        double _payTotal = payTotal.divide(BigDecimal.valueOf(100)).setScale(2, BigDecimal.ROUND_HALF_DOWN).doubleValue();

        log.print("当前订单号: " + orderNo +", 可退款余额: "+ _bal+", 可退款金额: "+ _pay+" , 在线支付的总金额:" + _payTotal);

      if (_bal<0 || _pay<0 ) return new Result().fail("订单异常");

        if (onilePayType > 3) {
            // 非线上则线上可退实付为0；
            _pay = .0;
        }

        String insertSql = " INSERT INTO {{?" + TD_TRAN_TRANS + "}} "
                + " (unqid, compid, orderno, payno, payprice, "
                + " payway, paysource, paystatus, payorderno, tppno, "
                + " paydate, paytime, completedate, completetime, cstatus) VALUES "
                + " (?,?,?,?,?, ?,?,?,?,?, CURRENT_DATE,CURRENT_TIME,NULL,NULL,?)";

        if(onilePayType == 4){
            //付款方式为线下转账
            _pay = .0;
            double _realBal = .0;
            boolean refamtResult = false;
            if(_bal>realrefamt){
                _realBal = realrefamt;
            }else{
                _realBal = _bal;
            }
            log.print("支付方式为线下支付时，余额应退款金额为:" + _bal + "  实际余额退款金额：" + _realBal + "   线上退款金额为：" + _pay);
            double _realbalAbs = Math.abs(_realBal);
            boolean isUpdate =  IceRemoteUtil.updateCompBal(compid, MathUtil.exactMul(_realbalAbs  , 100).intValue()) > 0;
            if (isUpdate){
                int rn = baseDao.updateNativeSharding(compid, year,insertSql,GenIdUtil.getUnqId(), compid, orderNo, 0, _realbalAbs * 100, 0,clientType,1,0,otherNo, 1024);
                if(rn >0){
                    refamtResult = afterSaleFinish(asno,realrefamt);
                }

                if(refamtResult){
                    return new Result().success("退款成功");
                }else{
                    return new Result().fail("退款失败");
                }

            }else{
                return new Result().fail("退款失败");
            }
        }
      if ((_bal + _pay) < realrefamt) return new Result().fail("超过可退款的金额, 当前可退款金额: "+ (_bal + _pay));

        String typeStr = onilePayType ==  1 ? "wxpay" : "alipay";
        boolean isApp = clientType == 1;

       double retpay  =  _pay - realrefamt;

        List<Object[]> params = new LinkedList<>();

        boolean isOk;

        BackResult r = null;

       if (retpay >= 0) {
           //只需要退款线上支付
           long refundno = GenIdUtil.getUnqId();
        HashMap<String,Object> map =  refund(typeStr, refundno+"",otherNo ,realrefamt ,_payTotal,isApp);
           String json = GsonUtils.javaBeanToJson(map);
           log.print("退款结果："+json);
           r = GsonUtils.jsonToJavaBean(json,BackResult.class);
           isOk = r.code == 2;

           if (isOk) {
//               //插入退款成功数据
               params.add(new Object[]{
                       GenIdUtil.getUnqId(),
                       compid, orderNo, refundno, realrefamt * 100,
                       onilePayType,clientType,1,0,otherNo, 1024
               });
           }
       }else{
           if (_pay > 0){
               long refundno = GenIdUtil.getUnqId();
               //先退线上(全额), 剩余部分退余额
               HashMap<String,Object> map  = refund(typeStr, GenIdUtil.getUnqId()+"",otherNo ,_pay ,_payTotal,isApp);
               String json = GsonUtils.javaBeanToJson(map);
               log.print("退款结果："+json);
               r = GsonUtils.jsonToJavaBean(json,BackResult.class);
               isOk = r.code == 2;
               if (isOk) {
//               //插入退款成功数据
                   params.add(new Object[]{
                           GenIdUtil.getUnqId(),
                           compid, orderNo, refundno, _pay * 100,
                           onilePayType,clientType,1,0,otherNo, 1024
                   });
               }
           }

           double _retpayAbs = Math.abs(retpay);
           isOk =  IceRemoteUtil.updateCompBal(compid, MathUtil.exactMul(_retpayAbs  , 100).intValue()) > 0;
           if (isOk){
               params.add(new Object[]{
                       GenIdUtil.getUnqId(),
                       compid, orderNo, 0, _retpayAbs * 100,
                       0,clientType,1,0,otherNo, 1024
               });
           }
       }

       if (params.size() > 0){
           baseDao.updateBatchNativeSharding(compid, year,insertSql, params, params.size());
       }

       String message = isOk ? "退款成功" : r.message;

       if (isOk) {
           if (onilePayType <= 3) {
               isOk = afterSaleFinish(asno,realrefamt);
               if (!isOk) message+=" ,订单修改失败";
           }
       }

       return isOk ? new Result().success(message) : new Result().fail(message);
    }

    /**
     * @接口摘要 售后完成更新状态
     * @业务场景 订单签收后确认签收
     * @传参类型 json
     * @传参列表 {asno 售后单号}
     * @返回列表 200 成功 -1 失败
     */
    public boolean afterSaleFinish(long asno,double realrefamt) {
        String queryOrderno = "select orderno,compid,pdno,pkgno from {{?" + DSMConst.TD_TRAN_ASAPP + "}} where asno = ? ";
        List<Object[]> queryRet = baseDao.queryNativeSharding(0, TimeUtils.getCurrentYear(), queryOrderno, asno);
        if (queryRet == null || queryRet.isEmpty()) {
            return false;
        }

        int year = Integer.parseInt("20" + queryRet.get(0)[0].toString().substring(0, 2));
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{-3, queryRet.get(0)[0], -2});
        params.add(new Object[]{200, queryRet.get(0)[2], 3, queryRet.get(0)[0], queryRet.get(0)[1],queryRet.get(0)[3]});

        log.print("售后订单："+ asno +"即将更新状态至 200！");
        baseDao.updateNativeSharding(0, TimeUtils.getCurrentYear(), UPD_ASAPP_CK_FINISH_SQL, 200, asno);
        int row  = baseDao.updateNativeSharding(0, TimeUtils.getCurrentYear(), UPDATE_ORDER_REFAMT, realrefamt * 100, asno);
        log.print("影响行数：" + row + "实际退款金额:"+realrefamt);
        boolean b = !ModelUtil.updateTransEmpty(baseDao.updateTransNativeSharding(Integer.parseInt(queryRet.get(0)[1].toString()), year,
                new String[]{UPD_ORDER_CK_SQL, UPD_ORDER_GOODS_CK_SQL}, params));

        return b;
    }
}
