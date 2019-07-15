package com.onek.global;

import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.util.FileServerUtils;
import dao.BaseDAO;
import util.GsonUtils;
import util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.onek.util.FileServerUtils.isFileExist;
import static constant.DSMConst.TD_NOTICE;

/**
 * @Author: leeping
 * @Date: 2019/5/8 11:03
 * 公告模块
 * 图片需要上传到公告资源主目录下
 *
 */
public class NoticeModule {

    private static class Param{
        int oid;
        String title;
        String type;
        String editor;
        String img;
        String date;
        String time;
        int priority;
        int cstatus;
    }

    /**
     * 添加公告
     * 需要确保图片上传成功再调用
     */
    @UserPermission(ignore = true)
    public Result add(AppContext context){
        try {
            String json = context.param.json;
            Param p = GsonUtils.jsonToJavaBean(json,Param.class); //CURRENT_DATE,CURRENT_TIME
            assert p != null;
            check(p);
            if (p.type.length()>6) return new Result().fail("类型长度不可超过6位");
            if (p.editor.length()>10) return new Result().fail("发布者名称不可超过10位");
            if (!isFileExist(p.img)) return new Result().fail("图片文件:"+ p.img +" 不存在!");
            //判断title是否重复
            String updateSql = "UPDATE {{?"+ TD_NOTICE+"}} SET type=?,editor=?,img=?,date=CURRENT_DATE,time=CURRENT_TIME,priority=?,cstatus=0 WHERE title = ?";
            int i = BaseDAO.getBaseDAO().updateNative(updateSql,
                   p.type,p.editor,p.img,p.priority, p.title
            );
            if (i<=0){
                String insertSql = "INSERT INTO {{?"+ TD_NOTICE+"}} (title, type,editor,img,date,time,priority) VALUES (?, ?, ?, ?,CURRENT_DATE,CURRENT_TIME,?)";
                i = BaseDAO.getBaseDAO().updateNative(insertSql,
                        p.title,p.type,p.editor,p.img,p.priority
                );
            }
            if (i > 0) return new Result().fail("添加成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("添加失败,请检查数据完整性");
    }

    private void check(Param p) {
        if (StringUtils.isEmpty(p.title,p.img)) throw new NullPointerException();
        if (StringUtils.isEmpty(p.editor)) p.editor = "一块医药";
        if (StringUtils.isEmpty(p.type)) {
            p.type = "";
        }else{
            p.type = "【"+p.type+"】";
        }
    }


    /**
     * 查询公告
     */
    @UserPermission(ignore = true)
    public Result query(AppContext context){
        List<Param> list = new ArrayList<>();
        try {
            String selectSql = "SELECT * FROM {{?"+TD_NOTICE+"}} WHERE cstatus&1=0 ORDER BY priority DESC,date DESC,time DESC"; //优先级+时间 倒叙
            List<Object[]> result = BaseDAO.getBaseDAO().queryNative(selectSql);
            Param[] params = new Param[result.size()];
            BaseDAO.getBaseDAO().convToEntity(result, params, Param.class);
            for (Param p : params){
                if (!isFileExist(p.img)){
                    continue;
                }
                p.img = FileServerUtils.fileDownloadPrev() + p.img;
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().success(list);
    }

    /**
     * 删除公告
     * 参数: oid
     * 软删除??
     */
    @UserPermission(ignore = true)
    public Result delete(AppContext context){
        try {
            String json = context.param.json;
            Param p = GsonUtils.jsonToJavaBean(json,Param.class);
            if (p!=null){
                String updateSql = "UPDATE {{?" +TD_NOTICE+ "}} SET cstatus=1 WHERE oid=?";
                int i = BaseDAO.getBaseDAO().updateNative(updateSql,p.oid);
                if (i>0) return new Result().success("删除成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new Result().fail("删除失败");
    }

}
