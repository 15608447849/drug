package com.onek.order;

import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.IOThreadUtils;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static constant.DSMConst.TD_FOOTPRINT;
import static constant.DSMConst.TD_TRAN_COLLE;
import static global.GenIdUtil.getUnqId;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/9 11:41
 */
public class MyFootprintModule {


    private static class Param {
        long unqid; //
        long sku;
        String data;//
        String time;//
    }


    /** 添加足迹
     * 参数 sku
     * */
    public Result add(AppContext appContext){

        int compId = appContext.getUserSession().compId;

        IOThreadUtils.runTask(() -> {
            deleteExpireData(compId);
        });

        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        long unqid = getUnqId();
        String insertSql = "INSERT INTO {{?"+TD_FOOTPRINT+"}} ( unqid, sku, compid, browsedate, browsetime ) " +
                "VALUES " +
                "( ?, ?, ?, CURRENT_DATE,CURRENT_TIME )";
        int i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
                insertSql,
                unqid, p.sku,compId);
        if (i > 0){
            return new Result().success("足迹记录成功");
        }
        return new Result().fail("足迹记录失败");
    }

    //删除足迹-逾期
    private void deleteExpireData(int compid){
            String delSql = "DELETE FROM {{?"+TD_FOOTPRINT+"}} WHERE TO_DAYS(NOW())-TO_DAYS(CONCAT(browsedate,' ',browsetime))>30 AND compid = ? ";
            BaseDAO.getBaseDAO().updateNativeSharding(compid,getCurrentYear(), delSql,compid);
    }

    /**
     * 删除足迹
     * 参数 unqid
     */
    public Result del(AppContext appContext){
        String json = appContext.param.json;
        Param p = GsonUtils.jsonToJavaBean(json, Param.class);
        assert p != null;
        int compId = appContext.getUserSession().compId;
        String delSql = "DELETE FROM {{?"+TD_FOOTPRINT+"}} WHERE unqid = ? ";
        int i =  BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(), delSql,compId);
        if (i > 0){
            return new Result().success("删除成功");
        }
        return new Result().fail("删除失败");
    }

    /**
     * 查询
     */
    public Result query(AppContext appContext){
        int compId = appContext.getUserSession().compId;
        String selectSql = "SELECT unqid,sku,browsedate,browsetime " +
                "FROM {{?"+TD_FOOTPRINT+"}} " +
                "WHERE compid = ?";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                selectSql,compId);
        assert lines!=null;
        ArrayList<Param> list = new ArrayList<>();
        Param data;
        for (Object[] arr: lines){
            data = new Param();
            data.unqid = StringUtils.checkObjectNull(arr[0],0L);
            data.sku = StringUtils.checkObjectNull(arr[1],0L);
            data.data = StringUtils.checkObjectNull(arr[2],"");
            data.time = StringUtils.checkObjectNull(arr[3],"");
            list.add(data);
        }
        return new Result().success(list);
    }

}
