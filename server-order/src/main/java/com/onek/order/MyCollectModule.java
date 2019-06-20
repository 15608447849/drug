package com.onek.order;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.IOThreadUtils;
import com.onek.util.IceRemoteUtil;
import com.onek.util.prod.ProdEntity;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.onek.util.CollectESUtil.*;
import static com.onek.util.GenIdUtil.getUnqId;
import static constant.DSMConst.TD_TRAN_COLLE;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/3 15:48
 */
public class MyCollectModule {
    private static class Param {
        long unqid;
        int promtype;
        float prize;
        long sku;
        String data;
        String time;

        ProdEntity info;
        String ids;
        public Param(){}
        public Param(String sku) {
            this.sku = Long.parseLong(sku);
        }
    }



    public Result check(AppContext appContext){
        boolean exist = false;
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        int compId = appContext.getUserSession().compId;
        if (compId>0){
            String selectSql = "SELECT unqid FROM {{?"+TD_TRAN_COLLE+"}} WHERE sku = ? AND compid = ?";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                    selectSql,p.sku,compId);
            assert lines != null;
            if (lines.size() > 0) exist = true;
        }
        return new Result().success(exist);
    }


    /**
     * 添加收藏
     * 参数: sku prize promtype
     */
    public Result add(AppContext appContext){
        int compId = appContext.getUserSession().compId;
        autoDelete(compId);
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        if (!StringUtils.isEmpty(p.ids)){
            String[] list = p.ids.split(",");
            for (String sku : list){
                add(new Param(sku),compId);
            }
        }
        if (p.sku > 0){
            add(p,compId);
        }
        return new Result().success("收藏成功");
    }

    public Result add(Param p,int compId){
        if (p.sku > 0) {

            //查询
            String selectSql = "SELECT unqid FROM {{?"+TD_TRAN_COLLE+"}} WHERE compid = ? AND sku = ?";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                    selectSql,compId,p.sku);
            int i;
            long unqid;
            if (lines.size()==1){
                unqid = StringUtils.checkObjectNull(lines.get(0)[0],0L);
                //更新
                String updateSql = "UPDATE {{?"+TD_TRAN_COLLE+"}} SET promtype=?,prize=?,createdate = CURRENT_DATE ,createtime = CURRENT_TIME WHERE unqid = ?";
                i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
                        updateSql,
                        p.promtype,p.prize * 100, unqid);
                if (i>0){
                    //更新缓存
                    updateCollectDocument(unqid,compId,p.promtype, (int) (p.prize * 100),p.sku);
                }
            }else{
                //插入
                unqid = getUnqId();
                String insertSql = "INSERT INTO {{?"+TD_TRAN_COLLE+"}} ( unqid, sku, prize, promtype, compid, createdate, createtime ) " +
                        "VALUES " +
                        "( ?, ?, ?, ?, ?, CURRENT_DATE,CURRENT_TIME )";

                i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
                        insertSql,
                        unqid, p.sku, p.prize * 100, p.promtype, compId);

                if (i>0){
                    //插入缓存
                    addCollectDocument(unqid,compId,p.promtype, (int) (p.prize * 100),p.sku);
                }
            }
            if (i > 0){
                return new Result().success("收藏成功");
            }
        }
        return new Result().success("收藏成功");
    }

    //自动删除
    private void autoDelete(int compId) {
        //如果超过指定条数 ,删除时间最小的数据 / 异步执行
        IOThreadUtils.runTask(() -> {
            String selectSql = "SELECT unqid,sku FROM {{?"+TD_TRAN_COLLE+"}} WHERE compid=? ORDER BY createdate,createtime";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),selectSql,compId);
            assert lines!=null;
            delOldData(lines,compId);
        });
    }

    private void delOldData(List<Object[]> lines,int compid) {
        if (lines.size()>20){
            Object[] arr = lines.remove(0);
            int i = delDataById((long)arr[0],(long)arr[1],compid);
            if (i > 0) {
                delOldData(lines, compid);
            }
        }
    }

    /**
     * 查询收藏
     */
    public Result query(AppContext appContext){
        int compId = appContext.getUserSession().compId;
       String selectSql = "SELECT unqid,sku,prize,promtype,createdate,createtime " +
               "FROM {{?"+TD_TRAN_COLLE+"}} " +
               "WHERE compid = ?";

        List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                selectSql,compId);
       assert lines!=null;
        ArrayList<Param>  list = new ArrayList<>();
        Param data;
       for (Object[] arr: lines){
           data = new Param();
           data.unqid = StringUtils.checkObjectNull(arr[0],0L);
           data.sku = StringUtils.checkObjectNull(arr[1],0L);
           int i = (int)arr[2];
           if (appContext.isAnonymous()){
               data.prize = -1;
           }else{
               data.prize = i/100.f;
           }
           data.promtype = StringUtils.checkObjectNull(arr[3],0);
           data.data = StringUtils.checkObjectNull(arr[4],"");
           data.time = StringUtils.checkObjectNull(arr[5],"");
           try {
               data.info = IceRemoteUtil.getProdBySku(data.sku);
           } catch (Exception e) {
//               e.printStackTrace();
           }
           list.add(data);
       }
        return new Result().success(list);
    }


    /**
     * 删除收藏
     * 参数 unqid
     */
    public Result del(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json,Param.class);
        assert p != null;
        int compId = appContext.getUserSession().compId;
        int i = delDataById(p.unqid,p.sku,compId);
        if (i > 0){
            return new Result().success("删除成功");
        }
        return new Result().fail("删除失败");
    }

    //根据ID删除数据
    private int delDataById(long unqid,long sku, int compid) {
        String delSql = "DELETE FROM {{?"+TD_TRAN_COLLE+"}} WHERE sku = ? AND compid = ?";
        deleteCollectDocument(unqid);
        return BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                delSql,
                sku, compid);
    }






    /**
     * app调用添加个人收藏
     * add by liaoz 2019年6月10日
     * @param appContext
     * @return
     */
    public Result appAddCollection(AppContext appContext){
        return add(appContext);
    }

    /**
     * app调用删除个人收藏
     * add by liaoz 2019年6月10日
     * @param appContext
     * @return
     */
    public Result appUpdCollection(AppContext appContext){
        return del(appContext);
    }


    /**
     * app调用查询个人收藏
     * add by liaoz 2019年6月10日
     * @param appContext
     * @return
     */
    public Result appQueryCollection(AppContext appContext){
        return query(appContext);
    }


}
