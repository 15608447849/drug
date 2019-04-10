package com.onek.order;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.IOThreadUtils;
import com.onek.util.prod.ProdEntity;
import com.onek.util.prod.ProduceStore;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import static constant.DSMConst.TD_TRAN_COLLE;
import static global.GenIdUtil.getUnqId;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/3 15:48
 */
public class MyCollectModule {
    private static class Param {
        long unqid;
        int promtype;
        int prize;
        long sku;
        String data;
        String time;
        String prodname;
    }

    public Result check(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        int compId = appContext.getUserSession().compId;
        String selectSql = "SELECT unqid FROM {{?"+TD_TRAN_COLLE+"}} WHERE sku = ? AND compid = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                selectSql,p.sku,compId);
        assert lines!=null;
        boolean exist = false;
        if (lines.size() > 0) exist = true;
        return new Result().success(exist);
    }


    /**
     * 添加收藏
     * 参数: sku prize promtype
     */
    public Result add(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        int compId = appContext.getUserSession().compId;
        long unqid = getUnqId();
        String insertSql = "INSERT INTO {{?"+TD_TRAN_COLLE+"}} ( unqid, sku, prize, promtype, compid, createdate, createtime ) " +
                "VALUES " +
                "( ?, ?, ?, ?, ?, CURRENT_DATE,CURRENT_TIME )";

       int i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
               insertSql,
               unqid, p.sku, p.prize, p.promtype, compId);

       if (i > 0){
           //如果超过指定条数 ,删除时间最小的数据 / 异步执行
           IOThreadUtils.runTask(() -> {
               String selectSql = "SELECT sku FROM {{?"+TD_TRAN_COLLE+"}} WHERE compid=? ORDER BY createdate,createtime";
               List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,compId);
               assert lines!=null;
               delOldData(lines,compId);
           });

           return new Result().success("收藏成功");
       }
        return new Result().fail("收藏失败");
    }

    private void delOldData(List<Object[]> lines,int compid) {
        if (lines.size()>20){
            Object[] arr = lines.remove(0);
            int i = delDataById((long)arr[0],compid);
            if (i > 0) delOldData(lines,compid);
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
           data.prize = StringUtils.checkObjectNull(arr[2],0);
           data.promtype = StringUtils.checkObjectNull(arr[3],0);
           data.data = StringUtils.checkObjectNull(arr[4],"");
           data.time = StringUtils.checkObjectNull(arr[5],"");
//           try {
//               ProdEntity entity = ProduceStore.getProdBySku(data.sku);
//               if (entity!=null && entity.getProdname()!=null){
//                   data.prodname = entity.getProdname();
//               }
//           } catch (Exception e) {
//               e.printStackTrace();
//           }
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
        int i = delDataById(p.sku,compId);
        if (i > 0){
            return new Result().success("删除成功");
        }
        return new Result().fail("删除失败");
    }

    //根据ID删除数据
    private int delDataById(long sku, int compid) {
        String delSql = "DELETE FROM {{?"+TD_TRAN_COLLE+"}} WHERE sku = ? AND compid = ?";
        return BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(),
                delSql,
                sku, compid);
    }
}
