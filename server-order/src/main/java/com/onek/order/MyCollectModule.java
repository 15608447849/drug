package com.onek.order;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static constant.DSMConst.TD_TRAN_COLLE;
import static global.GenIdUtil.getCompId;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/3 15:48
 */
public class MyCollectModule {
    private static class Param {
        long unqid;
        int  compid;
        int promtype;
        int prize;
        long sku;
        String prodname;
        String data;
        String time;
    }
    /**
     * 添加收藏
     */
    public Result add(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        long id = getCompId();
        String insertSql = "INSERT INTO {{?"+TD_TRAN_COLLE+"}} ( unqid, sku, prize, promtype, compid, prodname, createdate,createtime ) " +
                "VALUES " +
                "( ?, ?, ?, ?, ?, ?,CURRENT_DATE,CURRENT_TIME )";

       int i = BaseDAO.getBaseDAO().updateNativeSharding(p.compid,getCurrentYear(),
               insertSql,
               id, p.sku, p.prize, p.promtype, p.compid, p.prodname);

       if (i > 0){
           return new Result().success("收藏成功");
       }
        return new Result().fail("收藏失败");
    }
    /**
     * 查询收藏
     */
    public Result query(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json,Param.class);
        assert p != null;
       String selectSql = "SELECT unqid,sku,prize,promtype,compid,createdate,createtime,prodname " +
               "FROM {{?"+TD_TRAN_COLLE+"}} " +
               "WHERE compid = ?";

        List<Object[]> lines = BaseDAO.getBaseDAO().queryNative(selectSql,p.compid);
       assert lines!=null ;
        ArrayList<Param>  list = new ArrayList<>();
        Param data;
       for (Object[] arr: lines){
           data = new Param();
           data.unqid = StringUtils.checkObjectNull(arr[0],0L);
           data.sku = StringUtils.checkObjectNull(arr[1],0L);
           data.prize = StringUtils.checkObjectNull(arr[2],0);
           data.promtype = StringUtils.checkObjectNull(arr[3],0);
           data.compid = StringUtils.checkObjectNull(arr[4],0);
           data.data = StringUtils.checkObjectNull(arr[5],"");
           data.time = StringUtils.checkObjectNull(arr[6],"");
           data.prodname = StringUtils.checkObjectNull(arr[7],"");
           list.add(data);
       }
        return new Result().success(list);
    }


    /**
     * 删除收藏
     */
    public Result del(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json,Param.class);
        assert p != null;
        String delSql = "DELETE FROM {{?"+TD_TRAN_COLLE+"}} WHERE unqid = ? AND compid = ?";
        int i = BaseDAO.getBaseDAO().updateNativeSharding(p.compid,getCurrentYear(),
                delSql,
                p.unqid, p.compid);
        if (i > 0){
            return new Result().success("删除成功");
        }
        return new Result().fail("删除失败");
    }
}
