package com.onek.user;

import com.google.gson.Gson;
import com.onek.context.AppContext;
import com.onek.entitys.Result;
import com.onek.user.entity.InvoiceVO;
import com.onek.util.EmailUtil;
import com.onek.util.RandomUtil;
import constant.DSMConst;
import dao.BaseDAO;
import redis.util.RedisUtil;
import util.ArrayUtil;
import util.MathUtil;
import util.StringUtils;

import java.util.List;

/**
 * 我的发票模块
 */

public class MyInvoiceModule {
    private final static int EMAIL_REMAIN_TIME = 30 * 60; // 30分钟存活时间
    private final static String EMAIL_VAILD_SPILTER = "|";

    private final static String QUERY_INVOICE_BASE =
            "SELECT i.oid, i.cid, a.certificateno, i.bankers, i.account, i.tel, i.cstatus, i.email "
            + " FROM {{?" + DSMConst.TB_COMP_INVOICE + "}} i "
            + " LEFT JOIN {{?" + DSMConst.TB_COMP_APTITUDE + "}} a "
            + " ON a.cstatus&1 = 0 AND a.atype = 10 "
                + " AND i.cid = a.compid "
//                + " AND a.validitys <= CURRENT_DATE "
//                + " AND CURRENT_DATE <= a.validitye"
            + " WHERE i.cstatus&1 = 0 AND i.cid = ? ";

    private final static String INSERT_INVOICE =
            "INSERT INTO {{?" + DSMConst.TB_COMP_INVOICE + "}}"
            + " (cid, bankers, account, tel) "
            + " SELECT ?, ?, ?, ? "
            + " FROM DUAL "
            + " WHERE NOT EXISTS ("
                    + " SELECT * "
                    + " FROM {{?" + DSMConst.TB_COMP_INVOICE + "}}"
                    + " WHERE cid = ? ) ";

    private final static String UPDATE_INVOICE =
            "UPDATE {{?" + DSMConst.TB_COMP_INVOICE + "}}"
            + " SET bankers = ?, account = ?, tel = ? "
            + " WHERE cstatus&1 = 0 AND cid = ? ";

    private final static String UPDATE_EMAIL =
            "UPDATE {{?" + DSMConst.TB_COMP_INVOICE + "}}"
            + " SET email = ? "
            + " WHERE cstatus&1 = 0 AND cid = ? ";

    public Result getInvoice(AppContext appContext) {
        int compid = appContext.getUserSession().compId;

        List<Object[]> queryResult =
                BaseDAO.getBaseDAO().queryNative(QUERY_INVOICE_BASE, compid);

        InvoiceVO[] results = new InvoiceVO[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, results, InvoiceVO.class);

        return new Result().setQuery(results, null);
    }

    private static final String EMAIL_VALID_HEAD =
                "INVOICE_EMAIL_V_";

    private String genVaildKey(int compid) {
        return EMAIL_VALID_HEAD + compid;
    }

    public Result applyEmailVaildCode(AppContext appContext) {
        String[] params = appContext.param.arrays;
        int compid = appContext.getUserSession().compId;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数错误！");
        }

        String email = params[0];

        if (!StringUtils.isEmail(email)) {
            return new Result().fail("邮箱格式不正确！");
        }

        String vcode = RandomUtil.getRandomNumber(6);

        int setEmailValue = setEmail(compid, vcode, email);

        if (setEmailValue != 0) {
            if (setEmailValue == -1) {
                return new Result().fail("生成异常，请重新尝试生成！");
            }

            if (setEmailValue == -2) {
                return new Result().fail("请求频繁，请稍后重试！");
            }
        }

        // 发送验证邮箱
        boolean sendResult =
                EmailUtil.getEmailUtil().sendEmail(
                        "【一块医药】尊敬的用户：您正在绑定一块医药电子发票收件邮箱，验证码为："
                        + vcode + "，有效期30分钟。", email);

        if (!sendResult) {
            String key = genVaildKey(compid);
            RedisUtil.getStringProvide().delete(key);
            return new Result().fail("发送失败！");
        }

        return new Result().success("发送成功！");
    }

    private int setEmail(int compid, String vcode, String email) {
        String key = genVaildKey(compid);
        String value = vcode + EMAIL_VAILD_SPILTER + email;

        long remainTime =
                RedisUtil.getStringProvide().remainingSurvivalTime(key);

        if (remainTime > 0) {
            if (EMAIL_REMAIN_TIME - remainTime <= 60) {
                return -2;
            }
        }

        String setResult = RedisUtil.getStringProvide().set(key, value);

        if ("OK".equalsIgnoreCase(setResult)) {
            RedisUtil.getStringProvide().expire(key, EMAIL_REMAIN_TIME);
            return 0;
        }

        return -1;
    }

    private String getEmailByCode(int compid, String vcode) {
        if (StringUtils.isEmpty(vcode)) {
            return null;
        }

        String key = genVaildKey(compid);

        String vaildCode = RedisUtil.getStringProvide().get(key);

        if (!StringUtils.isEmpty(vaildCode)) {
            int index = vaildCode.indexOf(EMAIL_VAILD_SPILTER);
            if (vcode.equals(vaildCode.substring(0, index))) {
                RedisUtil.getStringProvide().delete(key);
                return vaildCode.substring(index + 1);
            }
        }

        return null;
    }

    public Result saveEmail(AppContext appContext) {
        int compId = appContext.getUserSession().compId;

        String[] params = appContext.param.arrays;

        if (ArrayUtil.isEmpty(params)) {
            return new Result().fail("参数错误！");
        }

        String email = getEmailByCode(compId, params[0]);

        if (StringUtils.isEmpty(email)) {
            return new Result().fail("验证码错误！");
        }

        if (!email.equals(params[1])) {
            return new Result().fail("当前邮箱与验证邮箱不一致！请重新获取验证码！");
        }

        int result =
                BaseDAO.getBaseDAO().updateNative(UPDATE_EMAIL, email, compId);

        return result > 0 ? new Result().success("操作成功") : new Result().fail("操作失败");
    }

    public Result saveInvoice(AppContext appContext) {
        int compId = appContext.getUserSession().compId;
        InvoiceVO frontVO = null;

        try {
            frontVO = new Gson()
                      .fromJson(appContext.param.json, InvoiceVO.class);

            if (frontVO == null) {
                throw new NullPointerException("VO is null!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Result().fail("参数错误！");
        }

        if (!MathUtil.isBetween(9, frontVO.getAccount().length(), 30)) {
            return new Result().fail("银行账号过长！");
        }
        if (!StringUtils.isBiggerZero(frontVO.getTel()) || frontVO.getTel().length() != 11) {
            return new Result().fail("电话号码不正确！");
        }
        if (!MathUtil.isBetween(0, frontVO.getBankers().length(), 20)) {
            return new Result().fail("开户行过长！");
        }
//        if (!MathUtil.isBetween(0, frontVO.getTaxpayer().length(), 20)) {
//            return new Result().fail("纳税人识别号过长！");
//        }

        InvoiceVO[] query = (InvoiceVO[]) getInvoice(appContext).data;

        int result = 0;

        try {
            if (query.length == 0) {
                // insert
                result = BaseDAO.getBaseDAO().updateNative(INSERT_INVOICE, compId,
                            frontVO.getBankers(), frontVO.getAccount (),
                            frontVO.getTel(), compId);
            } else {
                // update
                result = BaseDAO.getBaseDAO().updateNative(UPDATE_INVOICE,
                            frontVO.getBankers(), frontVO.getAccount (),
                            frontVO.getTel(), compId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result > 0 ? new Result().success("操作成功") : new Result().fail("操作失败");
    }
}
