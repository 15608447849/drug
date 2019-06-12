package com.onek.report.core;

import com.onek.report.col.ColGroup;
import com.onek.report.col.ColItem;
import com.onek.report.col.ColTotal;
import com.onek.report.vo.*;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Reporter {
    private static final Map<Long, String[]> areaStore = new HashMap<>();

    private static final String[][] GROUPS = {
        {
            " odate ",
            " (DAY(odate) + WEEKDAY(odate - INTERVAL DAY(odate) DAY)) DIV 7 + 1 ",
            " MONTH(odate) ",
            " YEAR(odate) ",
        },
        {
            " $ ",
            " 第$周 ",
            " $月 ",
            " $年 ",
        },
//        {
//            " CONCAT($) ",
//            " CONCAT('第', $, '周') ",
//            " CONCAT($, '月') ",
//            " CONCAT($, '年') ",
//        },
    };

    private static Comparator<ColGroup>[] COL_GROUP_COMPARATOR = new Comparator[] {
            new Comparator() {
                private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                @Override
                public int compare(Object o1, Object o2) {
                    ColGroup oo1 = (ColGroup) o1;
                    ColGroup oo2 = (ColGroup) o2;

                    try {
                        return sdf.parse(oo1.getDate()).compareTo(sdf.parse(oo2.getDate()));
                    } catch (ParseException e) {
                        return 0;
                    }
                }
            },

            new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    ColGroup oo1 = (ColGroup) o1;
                    ColGroup oo2 = (ColGroup) o2;
                    return Integer.parseInt(oo1.getDate()) - Integer.parseInt(oo2.getDate());
                }
            },
            new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    ColGroup oo1 = (ColGroup) o1;
                    ColGroup oo2 = (ColGroup) o2;
                    return Integer.parseInt(oo1.getDate()) - Integer.parseInt(oo2.getDate());
                }
            },
            new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    ColGroup oo1 = (ColGroup) o1;
                    ColGroup oo2 = (ColGroup) o2;
                    return Integer.parseInt(oo1.getDate()) - Integer.parseInt(oo2.getDate());
                }
            },
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
    private final String orderBy;
    private final Object[] params;
    private final Map<String, ColGroup> resultMap = new LinkedHashMap<>();

    private ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    private CountDownLatch countDownLatch = new CountDownLatch(5);

    public Reporter(int reportType, long areac, String date) {
        this.reportType = REPORTTYPE.values()[reportType];
        this.areac = areac;
        this.date = date;
        this.where = getWhere();
        this.params = getWhereParams();
        this.groupBy = getGroupBy();
        this.orderBy = getOrderBy();
    }

    public ColTotal getResult() {
        if (StringUtils.isEmpty(date)) {
            return null;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getCanceledNum();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getCancelVolume();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getSuccessNum();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getSuccessVolume();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    getCompPrice();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }
            }
        });

        try {
            boolean isTimeOut = !countDownLatch.await(10, TimeUnit.MINUTES);

            Map returnMap;

            if (isTimeOut) {
                returnMap = Collections.EMPTY_MAP;
            } else {
                returnMap = resultMap;
            }

            ColTotal returnCol = new ColTotal(new ArrayList<>(returnMap.values()));

            returnCol.getColGroups().sort(COL_GROUP_COMPARATOR[this.reportType.type]);

            if ((this.reportType.status&1) > 0) {
                accResult(returnCol);
            }

            return returnCol;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            executorService.shutdown();
            executorService = null;
        }

    }

    private void accResult(ColTotal colTotal) {
        List<ColGroup> groups = colTotal.getColGroups();

        if (groups == null || groups.size() <= 1) {
            return;
        }

        ColGroup last = groups.get(0);
        ColGroup curr;
        for (int i = 1; i < groups.size(); i++) {
            curr = groups.get(i);
            for (ColItem lastItem : last.getColItems()) {
                ColItem currItem = findReturnResult(curr, lastItem.getAreac());

                if (currItem == null) {
                    curr.getColItems().add(lastItem);
                } else {
                    currItem.accItem(lastItem);
                }

            }
        }

    }

    private void getSuccessVolume() {
        String sql =
                getSelectHead() + ", "
                        + " CONVERT (SUM(pdamt + freight) / 100, DECIMAL ( 65, 2 )) total, "
                        + " CONVERT (SUM( ao.refee ) / 100 , DECIMAL ( 65, 2 )) ret  "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} o "
                        + " LEFT JOIN ( SELECT orderno, SUM( a.refamt ) refee "
                                    + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} a "
                                    + " WHERE a.astype = 1 GROUP BY orderno) ao ON o.orderno = ao.orderno "
                        + where + " AND (ostatus > 0 OR ostatus = -2) AND settstatus > 0 "
                        + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        if (queryResult.isEmpty()) {
            return;
        }

        SuccessVolume[] canceledNums = new SuccessVolume[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, SuccessVolume.class);

        for (SuccessVolume canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }


    private void getCancelVolume() {
        String sql =
                getSelectHead() + ", "
                        + " CONVERT(SUM(IF(cstatus & 1024 > 0, (pdamt + freight), 0)) / 100, DECIMAL(65, 2)) bCancel, "
                        + " CONVERT(SUM(IF(cstatus & 1024 = 0, (pdamt + freight), 0)) / 100, DECIMAL(65, 2)) cCancel "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
                        + where + " AND ostatus = -4 "
                        + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        CancelVolume[] canceledNums = new CancelVolume[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, CancelVolume.class);

        for (CancelVolume canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private void getSuccessNum() {
        String sql =
                getSelectHead() + ", "
                + " COUNT( DISTINCT o.orderno ) total, "
                + " COUNT( ao.astype = 1 || NULL ) ret, "
                + " COUNT( DISTINCT ao.orderno ) back "
                + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} o "
                + " LEFT JOIN ( SELECT orderno, astype "
                        + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} a "
                        + " GROUP BY orderno, astype ) ao ON o.orderno = ao.orderno "
                + where + " AND (ostatus > 0 OR ostatus = -2) AND settstatus > 0 " + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        SuccessNum[] canceledNums = new SuccessNum[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, SuccessNum.class);

        for (SuccessNum canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private void getCompPrice() {
        String sql =
                getSelectHead() + ", "
                + " CONVERT(MAX(pdamt + freight) / 100, DECIMAL(65, 2)) max, "
                + " CONVERT(MIN(pdamt + freight) / 100, DECIMAL(65, 2)) min, "
                + " CONVERT(AVG(pdamt + freight) / 100, DECIMAL(65, 2)) avg "
                + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
                + where + " AND (ostatus > 0 OR ostatus = -2) AND settstatus > 0 " + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        CompPrice[] canceledNums = new CompPrice[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, CompPrice.class);

        for (CompPrice canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private String getSelectHead() {
        return " SELECT RPAD(LEFT(rvaddno, " + getAreaLeftNum() +  "), 12, 0) areac, "
                    + GROUPS[0][this.reportType.type];
    }

    private void getCanceledNum() {
        String sql =
            getSelectHead() + ", "
                + " COUNT( cstatus & 1024 > 0 || NULL ), "
                + " COUNT( cstatus & 1024 = 0 || NULL ) "
            + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
            + where + " AND ostatus = -4 "
            + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        CanceledNum[] canceledNums = new CanceledNum[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, CanceledNum.class);

        for (CanceledNum canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private synchronized void putMap(Object obj, long areac, String date) {
        ColGroup colGroup = resultMap.get(date);

        if (colGroup == null) {
            colGroup = new ColGroup();
            colGroup.setDate(date);
            colGroup.setDateLabel(
                    GROUPS[1][this.reportType.type].replace("$", date));

            resultMap.put(date, colGroup);
        }

        ColItem result = findReturnResult(colGroup, areac);

        if (result == null) {
            result = new ColItem(areac);
            colGroup.getColItems().add(result);
        }

        if (obj instanceof CanceledNum) {
            result.getOrderNum().setCanceledNum((CanceledNum) obj);
        } else if (obj instanceof CompPrice) {
            result.setCompPrice((CompPrice) obj);
        } else if (obj instanceof SuccessNum) {
            result.getOrderNum().setSuccessNum((SuccessNum) obj);
        } else if (obj instanceof CancelVolume) {
            result.getGmv().setCancelVolume((CancelVolume) obj);
        } else if (obj instanceof SuccessVolume) {
            result.getGmv().setSuccessVolume((SuccessVolume) obj);
        }
    }

    private ColItem findReturnResult(ColGroup colTotalLine, long areac) {
        if (colTotalLine == null) {
            return null;
        }

        List<ColItem> datas =colTotalLine.getColItems();

        for (ColItem data : datas) {
            if (data.getAreac() == areac) {
                return data;
            }
        }

        return null;
    }

    private String getGroupBy() {
        return " GROUP BY " + GROUPS[0][this.reportType.type]
                + " , LEFT(rvaddno, " + getAreaLeftNum() + ") ";
    }

    private String getOrderBy() {
        return " ORDER BY " + " LEFT(rvaddno, " + getAreaLeftNum() + "), "
                + GROUPS[0][this.reportType.type];
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
        int layer = areac == 0 ? -1 : AreaUtil.getLayer(areac);

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
        int layer = areac == 0 ? -1 : AreaUtil.getLayer(areac);

        return layer < 2 ? layer * 2 + 4 : (layer + 1) * 3;
    }

    private int getAreaLeftNum() {
        return getAreaLeftNum(this.areac);
    }

}
