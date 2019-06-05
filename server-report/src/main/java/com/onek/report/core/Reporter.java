package com.onek.report.core;

import com.onek.report.vo.CanceledNum;
import com.onek.report.vo.ReturnResult;
import com.onek.util.IceRemoteUtil;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.hyrdpf.ds.AppConfig;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Reporter {
    private static final String[] GROUPS = {
        " odate ",
        " (DAY(odate)+WEEKDAY(odate-INTERVAL DAY(odate) DAY)) DIV 7 + 1 ",
        " MONTH(odate) ",
        " YEAR(odate) ",
    };

    private enum REPORTTYPE {
        DAY_SIP(0, 0),
        DAY_ACC(1, 0),
        WEEK_SIP(0, 1),
        WEEK_ACC(1, 1),
        MONTH_SIP(0, 2),
        MONTH_ACC(1, 2),
        YEAR_SIP(0, 3);

        public int status;
        public int type;

        REPORTTYPE(int status, int type) {
            this.type = type;
            this.status = status;
        }
    }

    private final REPORTTYPE reportType;
    private final long areac;
    private final String date;
    private final String where;
    private final String groupBy;
    private final Object[] params;
    private final Map<Area, Map<String, ReturnResult>> resultMap = Collections.synchronizedMap(new LinkedHashMap<>());

    private ExecutorService executorService = Executors.newFixedThreadPool(10);
    private CountDownLatch countDownLatch = new CountDownLatch(5);

    public Reporter(int reportType, long areac, String date) {
        this.reportType = REPORTTYPE.values()[reportType];
        this.areac = areac;
        this.date = date;
        this.where = getWhere();
        this.params = getWhereParams();
        this.groupBy = getGroupBy();
    }

    private void getCanceledNum() {
        String sql =
            " SELECT RPAD(LEFT(rvaddno, " + getAreaLeftNum() +  "), 12, 0) areac, "
                    + GROUPS[this.reportType.type] + ", "
                    + " COUNT( ( ostatus = -4 && ( cstatus & 1024 ) > 0 ) || NULL ), "
                    + " COUNT( ( ostatus = -4 && ( cstatus & 1024 ) = 0 ) || NULL ) "
            + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
            + where
            + groupBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        CanceledNum[] canceledNums = new CanceledNum[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, CanceledNum.class);

        for (CanceledNum canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private void putMap(Object obj, long areac, String date) {
        Map<String, ReturnResult> map2 = findByAreac(areac);

        if (map2 == null) {
            Area a = new Area();
            a.setAreac(areac);
            map2 = new LinkedHashMap<>();
            resultMap.put(a, map2);
        }

        ReturnResult result = map2.get(date);

        if (result == null) {
            result = new ReturnResult();
            map2.put(date, result);
        }

        if (obj instanceof CanceledNum) {
            result.getOrderNum().setCanceledNum((CanceledNum) obj);
        }

    }

    private Map<String, ReturnResult> findByAreac(long areac) {
        Set<Area> keys = resultMap.keySet();
        Map<String, ReturnResult> result = null;
        for (Area key : keys) {
            if (key.getAreac() == areac) {
                result = resultMap.get(key);
                break;
            }
        }

        return result;
    }

    private String getGroupBy() {
        return " GROUP BY " + GROUPS[this.reportType.type]
                + " , LEFT(rvaddno, " + getAreaLeftNum() + ") ";
    }

    private String getWhere() {
        StringBuilder sb = new StringBuilder(" WHERE 1=1 ");

        sb.append(" AND rvaddno LIKE ? ");
        sb.append(" AND rvaddno <> ? " );
        if (date.contains("-")) {
            sb.append(" AND DATE_FORMAT(odate, '%Y-%m') = ? " );
        } else {
            sb.append(" AND DATE_FORMAT(odate, '%Y') = ? " );
        }

        return sb.toString();
    }

    private Object[] getWhereParams() {
        return new Object[] { getWhereLike(), this.areac, this.date };
    }

    private String getWhereLike(long areac) {
        int layer = AreaUtil.getLayer(areac);

        String like = String.valueOf(areac)
                .substring(0,
                        layer <= 2
                                ? layer * 2 + 2
                                : layer * 3);

        return like + "%";
    }

    private String getWhereLike() {
        return getWhereLike(this.areac);
    }

    private int getAreaLeftNum(long areac) {
        int layer = AreaUtil.getLayer(areac);

        return layer < 2 ? layer * 2 + 4 : (layer + 1) * 3;
    }

    private int getAreaLeftNum() {
        return getAreaLeftNum(this.areac);
    }

    static {
        /**初始化LOG4J2日志环境*/
        AppConfig.initLogger("log4j2.xml");
        /**初始化应用程序环境，如数据源等*/
        AppConfig.initialize();
    }


    public static void main(String[] args) {
        new Reporter(0, 430100000000L, "2019-05").getCanceledNum();
    }

    private class Area {
        long areac;
        String arean;
        int compNum;

        public Area() {}

        public Area(long areac) {
            this.areac = areac;
        }

        public long getAreac() {
            return areac;
        }

        public void setAreac(long areac) {
            if (this.areac == 0) {
                synchronized (Area.class) {
                    if (this.areac == 0) {
                        this.areac = areac;
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
//                                Area.this.arean = IceRemoteUtil.getArean(areac);

                                String sql =
                                        " SELECT COUNT(0) "
                                        + " FROM {{?" + DSMConst.TB_COMP + "}} "
                                        + " WHERE cstatus&1 = 0 AND ctype = 0 AND caddrcode LIKE ? ";

                                List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql, getWhereLike(Area.this.areac));

                                compNum = Integer.parseInt(queryResult.get(0)[0].toString());
                            }
                        });
                    }
                }
            }
        }

        public String getArean() {
            return arean;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Area area = (Area) o;

            return areac == area.areac;

        }

        @Override
        public int hashCode() {
            return (int) (areac ^ (areac >>> 32));
        }
    }
}
