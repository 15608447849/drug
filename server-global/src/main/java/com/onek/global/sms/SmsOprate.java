package com.onek.global.sms;

import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.util.LogUtil;
import com.onek.global.sms.entity.SmsTemplate;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 11842
 * @version 1.1.1
 * @description
 * @time 2019/3/19 16:28
 **/
public class SmsOprate {
    private static final BaseDAO baseDao = BaseDAO.getBaseDAO();

    private static ExecutorService executorService = Executors.newFixedThreadPool(2);

    private static SmsOprate smsOprate = new SmsOprate();

    public static SmsOprate getInstance() {
        return smsOprate;
    }

    /* *
     * @description 短信
     * @params [phone, smsContents]
     * @return void
     * @exception
     * @author 11842
     * @time  2019/3/19 16:50
     * @version 1.1.1
     **/
    public void sendMsmAsyn(final String[] paramList) {
        executorService.execute(() -> {
            try {
                if(paramList != null && paramList.length > 0){
                    sendSms(paramList);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @Description: 发送短信接口
     * 参数为list<String>类型
     * 参数为list<String>类型
     * 触发节点参考<<短信平台出发接点及短信模板>>
     * 第一个参数: 短信类型参照类TMPNO
     * 第二个参数手机号码
     * 第三个参数订单号码,如果没有就传""
     * 第四个参数订取货码,如果没有就传""
     * 第五个参数传公司名字,没有就传""
     * 第六个参数传司机姓名,没有就传""
     * 第七个参数传司机电话,没有就传""
     * 第八个参数传价格,没有就传"";"0"代表线下交易
     * 返回json:code = 0 成功 ,其他失败,msg有失败原因
     * @date: 2018/7/30
     */
    private void sendSms(String[] paramList) {
        try {
            int tno = Integer.parseInt(paramList[0]);
            SmsTemplate smsTemplate = getTmpByTno(tno);
            if (smsTemplate == null){
                LogUtil.getDefaultLogger().info("短信模板不存在！");
                return;
            }
            String content = smsTemplate.getTcontext();
            content = replaceContent(content, paramList);
            SmsUtil.getInstance().sendMsg(paramList[1], content);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /* *
     * @description 短信内容替换
     * @params [content]
     * @return java.lang.String
     * @exception
     * @author 11842
     * @time  2019/3/20 11:32
     * @version 1.1.1
     **/
    private String replaceContent(String content, String[] paramList) {

        return content;
    }

    private SmsTemplate getTmpByTno(int tno) {
        String selectSql = "select * from {{?" + DSMConst.D_SMS_TEMPLATE + "}} where cstatus&1=0 "
                + " and tno=" +tno;
        List<Object[]> queryResult = baseDao.queryNative(selectSql);
        if (queryResult == null || queryResult.isEmpty()) return null;
        SmsTemplate[] smsTemplates =new SmsTemplate[queryResult.size()];
        baseDao.convToEntity(queryResult, smsTemplates, SmsTemplate.class);
        return smsTemplates[0];
    }

    public void test() {
        String insertSql = "insert into {{?" + DSMConst.D_ORDER_BASE + "}} (orderno,remark,pubcompid,revcompid," +
                "pubdate,pubtime) values(?,?,?,?,CURRENT_DATE,CURRENT_TIME)";
        int re = baseDao.updateNativeSharding(536862720, 0, insertSql, 201903070000000003L,
                "dasda",536862720, 536870912);
        System.out.println(re);
    }
}
