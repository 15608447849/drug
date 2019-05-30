package com.onek.user;

import cn.hy.otms.rpcproxy.comm.cstruct.Page;
import cn.hy.otms.rpcproxy.comm.cstruct.PageHolder;
import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.onek.annotation.UserPermission;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.ProxyNoticeVO;
import com.onek.util.GenIdUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.GsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11842
 * @version 1.1.1
 * @description 区域广播
 * @time 2019/5/28 16:12
 **/
public class ProxyNoticeModule {

    private static BaseDAO baseDao = BaseDAO.getBaseDAO();
    
    /* *
     * @description 新增修改公告
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/28 16:21
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result optProxyNotice(AppContext appContext) {
        int code = 0;
        int optType = 0;
        long msgid = 0;
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        String proxyNotice = jsonObject.get("proxyNotice").getAsString();
        List<Long> areaList = JSON.parseArray(jsonObject.get("areaArr").getAsString()).toJavaList(Long.class);
        ProxyNoticeVO proxyNoticeVO = GsonUtils.jsonToJavaBean(proxyNotice, ProxyNoticeVO.class);
        assert proxyNoticeVO != null;
        String insertSQL = "insert into {{?" + DSMConst.TB_PROXY_NOTICE +"}} (msgid,sender,msgtxt,createdate,createtime,invdtime," +
                "readtimes,effcttime,revobj,cstatus) "
                + " values(?,?,?,CURRENT_DATE,CURRENT_TIME,"
                + "?,?,?,?,0)";
        String updateSQL = "update {{?" +DSMConst.TB_PROXY_NOTICE  + "}} set sender=?,msgtxt=?,invdtime=?,readtimes=?, "
                + "effcttime=?,revobj=? where cstatus&1=0 and msgid=?";
        if (proxyNoticeVO.getMsgid() > 0) {
            msgid = proxyNoticeVO.getMsgid();
            code = baseDao.updateNative(updateSQL,proxyNoticeVO.getSender(),proxyNoticeVO.getMsgtxt(),
                    proxyNoticeVO.getInvdtime(),proxyNoticeVO.getReadtimes(),proxyNoticeVO.getEffcttime(),
                    proxyNoticeVO.getRevobj(),proxyNoticeVO.getMsgid());
        } else {
            optType = 1;
            msgid = GenIdUtil.getUnqId();
            code = baseDao.updateNative(insertSQL, msgid, proxyNoticeVO.getSender(),proxyNoticeVO.getMsgtxt(),
                    proxyNoticeVO.getInvdtime(),proxyNoticeVO.getReadtimes(),proxyNoticeVO.getEffcttime(),
                    proxyNoticeVO.getRevobj());
        }
        optProxyArea(areaList, optType, msgid);
        return code > 0 ? result.success("操作成功！") : result.fail("操作失败！");
    }

    private void optProxyArea(List<Long> areaList, int optType, long msgid) {
        String insertAreaSQL = "insert into {{?" +  DSMConst.TB_PROXY_NOTICEAREC + "}} (unqid,msgid,areac,cstatus) "
                + " values(?,?,?,?)";
        List<Object[]> insertAreaParams = new ArrayList<>();
        List<Object[]> updateAreaParams = new ArrayList<>();

        if (optType == 1) {//新增
            for (long areaCode : areaList) {
                insertAreaParams.add(new Object[]{GenIdUtil.getUnqId(), msgid, areaCode,0});
            }
        } else {//修改
            String updateSQL = "update {{?" + DSMConst.TB_PROXY_NOTICEAREC + "}} set catstus=cstatus|1 "
                     + " where cstatus&1=0 and msgid=? and areac=?";
            List<Long> delArea = queryAreaByMsgId(msgid);
            for (int i = 0; i < areaList.size(); i++) {
                if (delArea.contains(areaList.get(i))) {
                    delArea.remove(i);
                }
                insertAreaParams.add(new Object[]{GenIdUtil.getUnqId(), msgid, areaList.get(i), 0});
            }
            for (long del : delArea) {
                updateAreaParams.add(new Object[]{msgid, del});
            }
            baseDao.updateBatchNative(updateSQL, updateAreaParams, updateAreaParams.size());
        }
        baseDao.updateBatchNative(insertAreaSQL, insertAreaParams, insertAreaParams.size());

    }

    private List<Long> queryAreaByMsgId(long msgid) {
        List<Long> areaCodeList = new ArrayList<>();
        String selectSQL = "select areac from {{?" + DSMConst.TB_PROXY_NOTICEAREC + "}} where cstatus&1=0 and "
                + " msgid=?";
        List<Object[]> qResult = baseDao.queryNative(selectSQL, msgid);
        if (qResult == null || qResult.isEmpty()) return areaCodeList;
        for (int i = 0; i < qResult.size(); i++) {
            areaCodeList.add(Long.parseLong(String.valueOf(qResult.get(i)[0])));
        }
        return areaCodeList;
    }


    /* *
     * @description 查询
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/28 18:49
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result queryNotice(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        Page page = new Page();
        page.pageSize = appContext.param.pageNumber;
        page.pageIndex = appContext.param.pageIndex;
        PageHolder pageHolder = new PageHolder(page);
        String title = jsonObject.get("title").getAsString();
        String selectSQL = "select msgid,sender,msgtxt,pubtime,invdtime,readtimes,revobj,createtime,cstatus from {{?"
                + DSMConst.TB_PROXY_NOTICE + "}} where cstatus&1=0 ";
        if (title != null && !title.isEmpty()) {
            selectSQL = selectSQL + " and  json_extract(msgtxt,'$.title') like '%" + title + "%'";
        }
        List<Object[]> queryResult = baseDao.queryNative(pageHolder, page, selectSQL);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        ProxyNoticeVO[] proxyNoticeVOS = new ProxyNoticeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, proxyNoticeVOS, ProxyNoticeVO.class);
        for (ProxyNoticeVO proxyNoticeVO : proxyNoticeVOS) {
            if (proxyNoticeVO.getMsgtxt() != null && !proxyNoticeVO.getMsgtxt().isEmpty()) {
                JsonObject msgtxt = jsonParser.parse(proxyNoticeVO.getMsgtxt()).getAsJsonObject();
                proxyNoticeVO.setTitle(msgtxt.get("title").getAsString());
            }
        }
        return result.setQuery(proxyNoticeVOS,pageHolder);
    }

    /* *
     * @description 详情
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/28 18:57
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result getNoticeDetail(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long msgid = jsonObject.get("msgid").getAsLong();
        String selectSQL = "select msgid,sender,msgtxt,pubtime,invdtime,readtimes,revobj,createtime,cstatus "
                + " from {{?" + DSMConst.TB_PROXY_NOTICE + "}} where cstatus&1=0 and msgid=?";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, msgid);
        if (queryResult == null || queryResult.isEmpty()) return result.success(null);
        ProxyNoticeVO[] proxyNoticeVOS = new ProxyNoticeVO[queryResult.size()];
        baseDao.convToEntity(queryResult, proxyNoticeVOS, ProxyNoticeVO.class);
        List<Long> areaList = queryAreaByMsgId(msgid);
        proxyNoticeVOS[0].setAreaList(areaList);
        if (proxyNoticeVOS[0].getMsgtxt() != null && !proxyNoticeVOS[0].getMsgtxt().isEmpty()) {
            JsonObject msgtxt = jsonParser.parse(proxyNoticeVOS[0].getMsgtxt()).getAsJsonObject();
            proxyNoticeVOS[0].setTitle(msgtxt.get("title").getAsString());
            proxyNoticeVOS[0].setContent(msgtxt.get("content").getAsString());
        }
        return result.success(proxyNoticeVOS[0]);
    }


    /* *
     * @description 消息接收操作
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/29 11:55
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result noticeReceive(AppContext appContext) {
        Result result = new Result();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long msgid = jsonObject.get("msgid").getAsLong();
        long receiver = jsonObject.get("receiver").getAsLong();
        String selectSQL = "select count(*) from {{?" + DSMConst.TB_PROXY_NOTICEDT + "}} where cstatus&1=0 "
                + " and msgid=? and receiver=?";
        String insertSQL = "insert into {{?" + DSMConst.TB_PROXY_NOTICEDT + "}} (unqid, msgid,receiver,readtimes,cstatus) "
                + " values(?,?,?,?,?)";
        String updSQL = "update {{?" +  DSMConst.TB_PROXY_NOTICEDT + "}} set readtimes+=readtimes where cstatus&1=0 "
                + " and msgid=? and receiver=?";
        List<Object[]> qResult = baseDao.queryNative(selectSQL, msgid, receiver);
        int code = 0;
        long count =Long.valueOf(String.valueOf(qResult.get(0)[0]));
        if (count > 0) {
            code = baseDao.updateNative(updSQL,  msgid, receiver);
        } else {
            code = baseDao.updateNative(insertSQL, GenIdUtil.getUnqId(), msgid, receiver, 1, 0);
        }
        return code > 0 ? result.success("操作成功！") : result.fail("操作失败！");
    }


    /* *
     * @description 获取公告
     * @params [appContext]
     * @return com.onek.entitys.Result
     * @exception
     * @author 11842
     * @time  2019/5/29 14:22
     * @version 1.1.1
     **/
    @UserPermission(ignore = false)
    public Result pushNotice(AppContext appContext) {
        Result result = new Result();
        List<String> msgList = new ArrayList<>();
        String json = appContext.param.json;
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
        long roleId =  jsonObject.get("roleid").getAsLong();//角色码
        long areac =  jsonObject.get("areac").getAsLong();//地区码
        int receiver = jsonObject.get("receiver").getAsInt();
        String selectSQL = "select msgtxt from {{?" + DSMConst.TB_PROXY_NOTICE + "}} a left join {{?"
                + DSMConst.TB_PROXY_NOTICEAREC + "}} b on a.msgid=b.msgid left join {{?"
                + DSMConst.TB_PROXY_NOTICEDT + "}} c on a.msgid=c.msgid where a.cstatus&1=0 and b.cstatus&1=0 "
                + " and c.cstatus&1=0 and revobj&?>0 and areac=? and receiver=? "
                + " and invdtime>CURRENT_TIMESTAMP and effcttime<=CURRENT_TIMESTAMP and (a.readtimes>c.readtimes "
                + " or a.readtimes=0) limit 0,3 order by effcttime desc and a.readtimes>0";
        List<Object[]> queryResult = baseDao.queryNative(selectSQL, roleId, areac, receiver);
        if (queryResult == null || queryResult.isEmpty()) return result.success(msgList);
        queryResult.forEach(qr -> {
            msgList.add(String.valueOf(qr[0]));
        });
        return result.success(msgList);
    }

}
