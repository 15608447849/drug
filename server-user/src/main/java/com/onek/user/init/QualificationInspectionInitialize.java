package com.onek.user.init;

import Ice.Application;
import com.onek.server.infimp.IIceInitialize;
import com.onek.util.SmsTempNo;
import com.onek.util.SmsUtil;
import dao.BaseDAO;
import org.jetbrains.annotations.NotNull;
import util.StringUtils;
import util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

import static com.onek.util.SmsTempNo.sendMessageToSpecify;
import static constant.DSMConst.*;

/**
 * @Author: leeping
 * @Date: 2019/5/13 17:43
 * 资质过去检测
 */
public class QualificationInspectionInitialize extends Thread implements IIceInitialize {
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Timer timer = new Timer();
    private static ArrayList<String> timerIdList = new ArrayList<>();

    private static class CusTimerTask extends TimerTask{
        private final int atype;
        private final int compid;
        private final String phone;
        private final boolean flag;

        private CusTimerTask(int atype, int compid,@NotNull String phone,boolean flag) {
            this.atype = atype;
            this.compid = compid;
            this.phone = phone;
            this.flag = flag;
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
            }else if (atype == 13){
                typeStr = "居民身份证";
            }
            if (typeStr == null) return;

            if(compid > 0 && compid < 536862720){
                SmsUtil.sendSmsBySystemTemp(phone, SmsTempNo.QUALIFICATION_PRE_EXPIRED,typeStr);
            }else{
                sendMessageToSpecify(compid,phone,SmsTempNo.QUALIFICATION_EXPIRED,typeStr);
            }

        }

        @Override
        public void run() {
            if (flag){
                //判断是否还在过期中
                final String sql = "SELECT * FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND validitye<CURRENT_DATE";

                final String presql = "SELECT * FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND  date_sub(validitye, interval 15 day) < CURRENT_DATE";

                List<Object[]> lines = null;
                if(compid > 0 && compid < 536862720){
                    lines = BaseDAO.getBaseDAO().queryNative(presql,atype,compid);

                  //  select 1 from tb_comp tbcp join tb_comp_aptitude tbca on tbcp.cid = tbca.compid where validitye<=CURRENT_DATE AND tbcp.cstatus & 128 = 0
//                    String cksql = "SELECT 1 FROM {{?"+ TB_COMP +"}} tbcp join {{?"+TB_COMP_APTITUDE+"}} tbca on tbcp.cid = tbca.compid " +
//                            " where validitye<=CURRENT_DATE AND tbcp.cstatus & 128 = 0 and cid = ? ";

  //                  List<Object[]> certRet = BaseDAO.getBaseDAO().queryNative(cksql,compid);
//                    if(certRet != null && !certRet.isEmpty()){
//                        String updateSql = "update {{?"+TB_COMP+"}} set cstatus = 128 where cid = ? ";
//                        //BaseDAO.getBaseDAO().updateNative(updateSql,compid);
//                    }
                }else{
                    lines = BaseDAO.getBaseDAO().queryNative(sql,atype,compid);
                }

                if (lines.size()>0){
                    sendMessage();//发送消息
                    timer.schedule(new CusTimerTask(atype,compid,phone,true),TimeUtils.PERIOD_MONTH);//设置下一次提醒时间 一个月后
                }else{
                    try {
                        lock.lock();
                        timerIdList.remove(atype+compid+phone);//移除记录
                        Application.communicator().getLogger().print("移除资质判断计时器任务:"+atype+compid+phone);
                    }finally {
                        lock.unlock();
                    }
                }
            }else{
                //判断是否今天过期
                final String sql = "SELECT * FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND validitye=CURRENT_DATE";
              //  final String presql = "SELECT * FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND  date_sub(validitye, interval 15 day) = CURRENT_DATE";
                final String presql = "SELECT * FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND  date_sub(validitye, interval 15 day) = CURRENT_DATE";

                List<Object[]> lines = null;
                if(compid > 0 && compid < 536862720){

                   // 128:审核中; 256:认证成功 512:认证失败;
                    lines = BaseDAO.getBaseDAO().queryNative(presql,atype,compid);
                    String cksql = "SELECT 1 FROM {{?"+ TB_COMP +"}} tbcp join {{?"+TB_COMP_APTITUDE+"}} tbca on tbcp.cid = tbca.compid " +
                            " where validitye<=CURRENT_DATE AND tbcp.cstatus & 128 = 0 and cid = ?";
                   // String cksql = "SELECT 1 FROM {{?"+ TB_COMP_APTITUDE +"}} WHERE atype = ? AND compid = ? AND validitye <= CURRENT_DATE AND cstatus & 128 = 0 ";
                    List<Object[]> certRet = BaseDAO.getBaseDAO().queryNative(cksql,compid);
                    if(certRet != null && !certRet.isEmpty()){
                        String updateSql = "update {{?"+TB_COMP+"}} set cstatus = 128 where cid = ? ";
                        BaseDAO.getBaseDAO().updateNative(updateSql,compid);
                    }
                }else{
                    lines = BaseDAO.getBaseDAO().queryNative(sql,atype,compid);
                }
                if (lines.size()>0){
                    sendMessage();//发送消息
                }
               try {
                   lock.lock();
                   //判断当前是否存在记录-存在不设置定时器任务
                   if (timerIdList.contains(atype+compid+phone)) return;
                   timerIdList.add(atype+compid+phone);//添加
                   Application.communicator().getLogger().print("添加资质判断计时器任务:"+atype+compid+phone);
               }finally {
                   lock.unlock();
               }
                //设置下一次提醒时间 一个月后
                timer.schedule(new CusTimerTask(atype,compid,phone,true),TimeUtils.PERIOD_MONTH);//设置下一次提醒时间
            }

        }
    }


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
           }
       }
    }

    //查询所有认证成功的企业
    //查询公司的资质到期时间
    //如果资质时间过时-发送消息 ,
    //设置定时器-一个月后执行
    //定时每次执行时,再次检测自己是否过期,过期-发送消息并再次添加定时任务,否则移除
    private void execute() {
        String sql = "SELECT atype,compid,uphone,validitye FROM {{?" + TB_COMP_APTITUDE + "}} AS a INNER JOIN {{?" + TB_SYSTEM_USER + "}} AS b ON a.compid=b.cid WHERE compid IN ( SELECT cid FROM {{?" + TB_COMP + "}} WHERE cstatus&256=256 AND ctype=0 )" +
                " AND validitye <= CURRENT_DATE " +
                " UNION " +
                " SELECT atype,compid,uphone,validitye FROM {{?" + TB_COMP_APTITUDE + "}} AS a INNER JOIN {{?" + TB_SYSTEM_USER + "}} AS b ON a.compid=b.cid WHERE compid IN ( SELECT cid FROM {{?" + TB_COMP + "}} WHERE  ctype in (1,2) ) " +
                " AND (date_sub(validitye, interval 15 day) < CURRENT_DATE or validitye <= CURRENT_DATE)" +
                "  AND  b.roleid & 2048 > 0 ";

        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(sql);
        for (Object[] row : lines){
            checkTypeAndSendMsg(row);
        }
    }

    private void checkTypeAndSendMsg(Object[] row) {
        int type = StringUtils.checkObjectNull(row[0],0);
        int compid = StringUtils.checkObjectNull(row[1],0);
        String uphone = StringUtils.obj2Str(row[2]);
        timer.schedule(new CusTimerTask(type,compid,uphone,false),60 * 1000); //延时一分钟执行
    }

}
