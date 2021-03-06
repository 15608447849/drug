package com.onek.order;

import com.onek.annotation.UserPermission;
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

import static com.onek.order.MyCollectModule.setPrice;
import static com.onek.util.GenIdUtil.getUnqId;
import static constant.DSMConst.TD_FOOTPRINT;
import static util.TimeUtils.getCurrentYear;

/**
 * @Author: leeping
 * @Date: 2019/4/9 11:41
 */
public class MyFootprintModule {


    private static class Param {
        String compid;
        String unqid; //
        String sku;
        String date;//
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

        if(!StringUtils.isEmpty(p.sku)){
            String selectSql = "SELECT unqid FROM {{?"+TD_FOOTPRINT+"}} " +
                    "WHERE compid = ? AND sku = ?";
            List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                    selectSql,compId,p.sku);
            if (lines.size() == 1){
                //修改
                String unqid =  lines.get(0)[0].toString();
                String updateSql = "UPDATE {{?"+TD_FOOTPRINT+"}} SET browsedate = CURRENT_DATE ,browsetime = CURRENT_TIME WHERE unqid = ?";
                int i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
                        updateSql,
                        unqid);
                if (i > 0){
                    return new Result().success("足迹修改成功");
                }
            } else{
                //插入
                long unqid = getUnqId();
                String insertSql = "INSERT INTO {{?"+TD_FOOTPRINT+"}} ( unqid, sku, compid, browsedate, browsetime ) " +
                        "VALUES " +
                        "( ?, ?, ?, CURRENT_DATE,CURRENT_TIME )";
                int i = BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(),
                        insertSql,
                        unqid, p.sku,compId);
                if (i > 0){
                    return new Result().success("足迹添加成功");
                }
            }
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
        int compId = appContext.getUserSession().compId;
        String json = appContext.param.json;
        List<String> list = GsonUtils.json2List(json,String.class);
        if (list!=null && list.size() > 0){
            String delSql = "DELETE FROM {{?"+TD_FOOTPRINT+"}} WHERE sku = ? AND compid = ?";
           for (String sku : list){
               int i =  BaseDAO.getBaseDAO().updateNativeSharding(compId,getCurrentYear(), delSql, sku,compId);
               if (i <= 0){
                   return new Result().fail("删除失败");
               }
           }
            return new Result().success("商品足迹移除成功","删除成功");
        }

        return new Result().fail("商品足迹移除失败");
    }

    private class ResultItem{
        String date; //日期
        List<ProdEntity> list = new ArrayList<>(); //商品信息
    }

    //后台查询
    @UserPermission(ignore = true)
    public List<String> backQuery(AppContext appContext){
        List<String> list = new ArrayList<>();
        try {
            int compId  = Integer.parseInt(appContext.param.arrays[0]);
            List<Param> plist = selectInfoByComp(compId,null);
            for (Param p : plist) list.add(p.sku);
        } catch (Exception ignored) {
        }
        return list;
    }

    private List<Param> selectInfoByComp(int compId,String dateStr){

        if (StringUtils.isEmpty(dateStr)) dateStr = "CURRENT_DATE";

        ArrayList<Param> list = new ArrayList<>();

        String selectSql = "SELECT unqid,sku,browsedate,browsetime " +
                "FROM {{?"+TD_FOOTPRINT+"}} " +
                "WHERE compid = ? AND browsedate<= ? ORDER BY browsedate DESC,browsetime DESC";
        List<Object[]> lines = BaseDAO.getBaseDAO().queryNativeSharding(compId,getCurrentYear(),
                selectSql,compId,dateStr);
        assert lines!=null;
        Param data;
        for (Object[] arr: lines){
            data = new Param();
            data.compid = compId+"";
            data.unqid = StringUtils.checkObjectNull(arr[0],"");
            data.sku = StringUtils.checkObjectNull(arr[1],"");
            data.date = StringUtils.checkObjectNull(arr[2],"");
            data.time = StringUtils.checkObjectNull(arr[3],"");
            list.add(data);
        }
        return list;
    }


    /**
     * 功能:查询足迹
     * 参数类型:json
     * 参数集: date=yyyy-MM-dd
     * 返回值:
     * 详情说明:
     */
    public Result query(AppContext appContext){
        List<ResultItem> items = new ArrayList<>();
        try {
            Param query = GsonUtils.jsonToJavaBean(appContext.param.json,Param.class);
            String date = null;
            if (query != null) {
                date = query.date;
            }
            int compId = appContext.getUserSession().compId;
            List<Param> list = selectInfoByComp(compId,date);
            for (Param it : list){
                ResultItem rit = null;
                boolean isAdd = true;
                for (ResultItem item : items){
                    if (item.date.equals(it.date)){
                        rit = item;
                        isAdd = false;
                        break;
                    }
                }
                if (isAdd){
                    rit = new ResultItem();
                    rit.date = it.date;
                    items.add(rit);
                }
                try {
                    /**
                     * add by liaozhou 2019年6月19日
                     * 判断查看我的足迹时登陆账号是否已认证，认证显示价格，反之显示未认证
                     */
                    ProdEntity prod =  IceRemoteUtil.getProdBySku(Long.parseLong(it.sku));
                    if (prod!=null){
                        setPrice(appContext, prod);
                    }else{
                        continue;
                    }
                    /*add end */
                    rit.list.add(prod);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().success(items);
    }

}
