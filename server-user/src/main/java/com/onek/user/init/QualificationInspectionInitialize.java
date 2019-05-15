package com.onek.user.init;

import com.onek.server.infimp.IIceInitialize;
import com.onek.util.SmsTempNo;
import dao.BaseDAO;
import org.jetbrains.annotations.NotNull;
import util.StringUtils;
import util.TimeUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.onek.util.SmsTempNo.sendMessageToSpecify;
import static constant.DSMConst.*;

/**
 * @Author: leeping
 * @Date: 2019/5/13 17:43
 * 资质过去检测
 */
public class QualificationInspectionInitialize extends Thread implements IIceInitialize {


    private static class CusTimerTask extends TimerTask{
        private static final String sql = "select * FROM {{?"+D_COMP_APTITUDE+"}} WHERE atype = ? AND compid = ? AND validitye < CURRENT_DATE";
        private final int atype;
        private final int compid;
        private final String phone;

        private CusTimerTask(int atype, int compid,@NotNull String phone,boolean isSend) {
            this.atype = atype;
            this.compid = compid;
            this.phone = phone;
            if (isSend){
                sendMessage();
            }
        }

        private void sendMessage() {
            String typeStr = null;
            //10-营业执照 11-药店经营许可证 12-gsp认证
            if (atype == 10){
                typeStr = "营业执照";
            }else if (atype == 11){
                typeStr = "药店经营许可证";
            }else if (atype == 12){
                typeStr = "gsp认证";
            }

            if (typeStr == null) return;
            sendMessageToSpecify(compid,phone,SmsTempNo.QUALIFICATION_EXPIRED,typeStr);
        }

        @Override
        public void run() {
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql,atype,compid);
            if (lines.size()>0){
                //再次执行
                timer.schedule(new CusTimerTask(atype,compid,phone,true),TimeUtils.PERIOD_MONTH);
            }
        }
    }

    private static final Timer timer = new Timer();

    @Override
    public void startUp(String serverName) {
        this.setName("QI-Thread-"+getId());
        this.start();
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public void run() {

       while (true){
           try {
               execute();
               Thread.sleep(TimeUtils.PERIOD_DAY);
           }catch (Exception ignored){
               try {
                   Thread.sleep(5 * 60 * 1000);
               } catch (InterruptedException ignored2) {
               }
           }
       }
    }

    //查询所有认证成功的企业
    //查询公司的资质到期时间
    //如果资质时间过时-发送消息 ,
    //设置定时器-一个月后执行
    //定时每次执行时,再次检测自己是否过期,过期-发送消息并再次添加定时任务,否则移除
    private void execute() {
        String sql = "SELECT atype,compid,uphone FROM {{?" + D_COMP_APTITUDE + "}} AS a INNER JOIN {{?" + D_SYSTEM_USER + "}} AS b ON a.compid=b.cid WHERE compid IN ( SELECT cid FROM {{?" + D_COMP + "}} WHERE cstatus&256=256 AND ctype=0 ) AND validitye < CURRENT_DATE";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql);
        for (Object[] row : lines){
            checkTypeAndSendMsg(row);
        }
    }

    private void checkTypeAndSendMsg(Object[] row) {
        int type = StringUtils.checkObjectNull(row[0],0);
        int compid = StringUtils.checkObjectNull(row[1],0);
        String uphone = StringUtils.obj2Str(row[2]);
        timer.schedule(new CusTimerTask(type,compid,uphone,false),0);
    }

}
