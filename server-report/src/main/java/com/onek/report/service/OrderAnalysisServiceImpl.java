package com.onek.report.service;

import com.onek.report.col.ColGroup;
import com.onek.report.col.ColItem;
import com.onek.report.col.ColTotal;
import com.onek.report.vo.*;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.hyrdpf.ds.AppConfig;
import util.MathUtil;
import util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.onek.util.fs.FileServerUtils.getExcelDownPath;

public class OrderAnalysisServiceImpl {
//    static {
//        /**初始化LOG4J2日志环境*/
//        AppConfig.initLogger("log4j2.xml");
//        /**初始化应用程序环境，如数据源等*/
//        AppConfig.initialize();
//    }
//
//    public static void main(String[] args) {
//        new OrderAnalysisServiceImpl(5, 430000000000L, "2019-06").getResult();
//    }


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
        DAY_SIP(0, 0, "天报"),
        DAY_ACC(1, 0, "天报(累计)"),
        WEEK_SIP(0, 1, "周报"),
        WEEK_ACC(1, 1, "周报(累计)"),
        MONTH_SIP(0, 2, "月报"),
        MONTH_ACC(1, 2, "月报(累计)"),
        YEAR_SIP(0, 3, "年报");

        public int status;
        public int type;
        public String name;

        REPORTTYPE(int status, int type, String name) {
            this.type = type;
            this.status = status;
            this.name = name;
        }
    }

    private final REPORTTYPE reportType;
    private final long areac;
    private final String date;
    private final String where;
    private final String groupBy;
    private final String orderBy;
    private final Object[] params;
    private final String fileName;
    private final Map<String, ColGroup> resultMap = new LinkedHashMap<>();

    private ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    private CountDownLatch countDownLatch = new CountDownLatch(5);

    public OrderAnalysisServiceImpl(int reportType, long areac, String date) {
        this.reportType = REPORTTYPE.values()[reportType];
        this.areac = areac;
        this.date = date;
        this.where = getWhere();
        this.params = getWhereParams();
        this.groupBy = getGroupBy();
        this.orderBy = getOrderBy();
        this.fileName = "订单报表-" + this.reportType.name + "_" + date;
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

            boolean isAcc = (this.reportType.status & 1) > 0;

            ColTotal returnCol = new ColTotal(
                    new ArrayList<>(returnMap.values()), isAcc);

            returnCol.getColGroups().sort(COL_GROUP_COMPARATOR[this.reportType.type]);

            if (isAcc) {
                accResult(returnCol);
            }

            if (this.reportType == REPORTTYPE.MONTH_ACC && this.date.contains("-")) {
                Iterator<ColGroup> it = returnCol.getColGroups().iterator();
                int len = returnCol.getColGroups().size();
                while (it.hasNext()) {
                    it.next();

                    if (--len > 0) {
                        it.remove();
                    }
                }
            }

            for (ColGroup colGroup : returnCol.getColGroups()) {
                for (ColItem colItem : colGroup.getColItems()) {
                    colItem.getCompPrice().setAvg(
                            MathUtil.exactDiv(
                                    colItem.getGmv().getTotal(),
                                    colItem.getOrderNum().getTotal()).doubleValue()
                    );
                }
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

        ColGroup lastGroup = groups.get(0);
        ColGroup currGroup;
        for (int i = 1; i < groups.size(); i++) {
            currGroup = groups.get(i);
            for (ColItem lastItem : lastGroup.getColItems()) {
                ColItem currItem = findReturnResult(currGroup, lastItem.getAreac());

                if (currItem == null) {
                    currGroup.getColItems().add(lastItem);
                } else {
                    currItem.accItem(lastItem);
                }
            }

            lastGroup = currGroup;
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
                        + " WHERE a.cstatus&1 = 0  AND (a.ckstatus = 1 OR a.ckstatus = 200) "
                        + " GROUP BY orderno, astype ) ao ON o.orderno = ao.orderno "
                        + where + " AND (ostatus > 0 OR ostatus = -2 OR ostatus = -3) AND settstatus > 0 "
                        + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        SuccessNum[] canceledNums = new SuccessNum[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, SuccessNum.class);

        for (SuccessNum canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private void getSuccessVolume() {
        String sql =
                getSelectHead() + ", "
                        + " CONVERT (SUM(pdamt + freight) / 100, DECIMAL ( 65, 2 )) total, "
                        + " CONVERT (SUM( ao.refee ) / 100 , DECIMAL ( 65, 2 )) ret  "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} o "
                        + " LEFT JOIN ( SELECT orderno, SUM( IFNULL(a.refamt, 0) ) refee "
                        + " FROM {{?" + DSMConst.TD_TRAN_ASAPP + "}} a "
                        + " WHERE a.cstatus&1 = 0 AND (a.ckstatus = 1 OR a.ckstatus = 200) "
                        + " GROUP BY orderno) ao ON o.orderno = ao.orderno "
                        + where + " AND (ostatus > 0 OR ostatus = -2 OR ostatus = -3) AND settstatus > 0 "
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

    private void getCompPrice() {
        String sql =
                getSelectHead() + ", "
                        + " CONVERT(MAX(pdamt + freight) / 100, DECIMAL(65, 2)) max, "
                        + " CONVERT(MIN(pdamt + freight) / 100, DECIMAL(65, 2)) min, "
                        + " CONVERT(AVG(pdamt + freight) / 100, DECIMAL(65, 2)) avg "
                        + " FROM {{?" + DSMConst.TD_BK_TRAN_ORDER + "}} "
                        + where + " AND (((ostatus > 0 OR ostatus = -2 OR ostatus = -3) AND settstatus > 0) OR ostatus = -4) " + groupBy + this.orderBy;

        List<Object[]> queryResult = BaseDAO.getBaseDAO()
                .queryNativeSharding(Integer.parseInt(date.split("-")[0]), 0, sql, params);

        CompPrice[] canceledNums = new CompPrice[queryResult.size()];

        BaseDAO.getBaseDAO().convToEntity(queryResult, canceledNums, CompPrice.class);

        for (CompPrice canceledNum : canceledNums) {
            putMap(canceledNum, canceledNum.getAreac(), canceledNum.getDate());
        }
    }

    private String getSelectHead() {
        return " SELECT RPAD(LEFT(rvaddno, " + getAreaLeftNum() + "), 12, 0) areac, "
                + GROUPS[0][this.reportType.type];
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

        List<ColItem> datas = colTotalLine.getColItems();

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

        if (!isBottom()) {
            sb.append(" AND rvaddno LIKE ? ");
            sb.append(" AND rvaddno <> ? ");
        } else {
            sb.append(" AND rvaddno = ? ");
        }

        if (date.contains("-")) {
            if (this.reportType == REPORTTYPE.MONTH_ACC) {
                sb.append(" AND DATE_FORMAT(odate, '%Y-%m') <= ? ");
            } else {
                sb.append(" AND DATE_FORMAT(odate, '%Y-%m') = ? ");
            }
        } else {
            sb.append(" AND DATE_FORMAT(odate, '%Y') = ? ");
        }

        return sb.toString();
    }

    private boolean isBottom() {
        return this.areac > 0 && AreaUtil.getLayer(this.areac) >= 2;
    }

    private Object[] getWhereParams() {
        return isBottom()
                ? new Object[] { this.areac, this.date }
                : new Object[] { getWhereLike(), this.areac, this.date };
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

    private static final Object[][][] eachIndex = new Object[][][] {
            { { 0, "时间" },{ 1, "地区" }, { 2, "客户数" }, { 3, "交易订单数" }, { 13, "GMV" }, { 21, "客单价" } },

            { { 3, "交易取消订单数" }, { 6, "交易成功订单数" }, { 11, "小计" }, { 12, "订单成交率" }, { 13, "交易取消金额" },
                { 16, "交易成交金额" }, { 19, "小计" }, { 20, "金额成交率" }, { 21, "最高" }, { 22, "最低" },
                { 23, "平均" } },

            { {3, "客户取消"}, {4, "客服取消"}, {5, "小计"}, {6, "交易成功"}, {7, "退货"},
                    {8, "售后"}, {9, "退货率"}, {10, "售后率"}, {13, "客户取消"}, {14, "客服取消"},
                    {15, "小计"}, {16, "交易成功"}, {17, "退款"}, {18, "退款率"} },
    };

    private void createTableHead(HSSFSheet sheet) {
        HSSFRow row;
        HSSFCell cell;

        Object[][] param;
        for (int i = 0; i < eachIndex.length; i++) {
            param = eachIndex[i];
            row = sheet.createRow(i);
            for (Object[] objects : param) {
                cell = row.createCell(Integer.parseInt(objects[0].toString()));
                cell.setCellValue(objects[1].toString());
            }
        }

        addMergedRegion(new CellRangeAddress(0, 2, 0, 0), sheet);
        addMergedRegion(new CellRangeAddress(0, 2, 1, 1), sheet);
        addMergedRegion(new CellRangeAddress(0, 2, 2, 2), sheet);

        addMergedRegion(new CellRangeAddress(0, 0, 3, 12), sheet);
        addMergedRegion(new CellRangeAddress(0, 0, 13, 20), sheet);
        addMergedRegion(new CellRangeAddress(0, 0, 21, 23), sheet);

        addMergedRegion(new CellRangeAddress(1, 1, 3, 5), sheet);
        addMergedRegion(new CellRangeAddress(1, 1, 6, 10), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 11, 11), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 12, 12), sheet);
        addMergedRegion(new CellRangeAddress(1, 1, 13, 15), sheet);
        addMergedRegion(new CellRangeAddress(1, 1, 16, 18), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 19, 19), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 20, 20), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 21, 21), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 22, 22), sheet);
        addMergedRegion(new CellRangeAddress(1, 2, 23, 23), sheet);
    }

    private void addMergedRegion(
            CellRangeAddress cellRangeAddress,
            HSSFSheet sheet) {
        sheet.addMergedRegion(cellRangeAddress);
        setBorderStyle(HSSFCellStyle.BORDER_THIN, cellRangeAddress, sheet);
    }

    public String getExportPath() {
        ColTotal colTotal = getResult();

        if (colTotal == null) {
            return null;
        }

        try (HSSFWorkbook hwb = new HSSFWorkbook()) {
            HSSFSheet sheet = hwb.createSheet();

            createTableHead(sheet);

            int currRow = 3;
            List<ColGroup> groups = colTotal.getColGroups();

            if (groups == null || groups.isEmpty()) {
                return null;
            }

            for (ColGroup group : groups) {
                currRow = insertGroup(currRow, group, sheet);
            }

            HSSFRow row = sheet.createRow(currRow);
            insertCell(0, row, "总计");
            addMergedRegion(new CellRangeAddress(currRow, currRow, 0, 2), sheet);
            insertCols(3, colTotal.getEachCol(), row);

            HSSFCellStyle style = hwb.createCellStyle();
            style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
            style.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
            style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
            style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
            style.setBorderRight(HSSFCellStyle.BORDER_THIN);
            style.setBorderTop(HSSFCellStyle.BORDER_THIN);
            style.setTopBorderColor(HSSFColor.BLACK.index);
            style.setBottomBorderColor(HSSFColor.BLACK.index);
            style.setLeftBorderColor(HSSFColor.BLACK.index);
            style.setRightBorderColor(HSSFColor.BLACK.index);

            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);

                for (int j = 0; j <= row.getLastCellNum(); j++) {
                    HSSFCell cell = row.getCell(j);
                    if (cell != null) {
                        cell.setCellStyle(style);
                    }
                }
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
                hwb.write(bos);

                String title = getExcelDownPath(
                        this.fileName, new ByteArrayInputStream(bos.toByteArray()));

                return title;
            }

            /*try (FileOutputStream bos = new FileOutputStream("D:/demo.xls")) {
                hwb.write(bos);
            }*/



        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void setBorderStyle(int border, CellRangeAddress region, HSSFSheet sheet){
        if (region != null) {
            RegionUtil.setBorderBottom(border, region, sheet, sheet.getWorkbook());  //下边框
            RegionUtil.setBorderLeft(border, region, sheet, sheet.getWorkbook());     //左边框
            RegionUtil.setBorderRight(border, region, sheet, sheet.getWorkbook());    //右边框
            RegionUtil.setBorderTop(border, region, sheet, sheet.getWorkbook());      //上边框
        }
    }

    private int insertGroup(int startRow, ColGroup group, HSSFSheet sheet) {
        List<ColItem> items = group.getColItems();

        if (items == null || items.isEmpty()) {
            return -1;
        }

        int currRow = startRow;
        int len = items.size();
        HSSFRow row;
        for (int i = 0; i < len; i++) {
            row = sheet.createRow(currRow++);

            if (i == 0) {
                insertCell(0, row, group.getDateLabel());
            }

            insertItem(items.get(i), row);
        }

        if (startRow < currRow - 1) {
            addMergedRegion(new CellRangeAddress(startRow, currRow - 1, 0, 0), sheet);
        }

        row = sheet.createRow(currRow++);
        insertCell(0, row, "合计");
        insertCell(1, row, group.getGroupLabel());
//        insertCell(2, row, group.getCompTotal());
        insertCols(3, group.getEachCol(), row);
        addMergedRegion(new CellRangeAddress(currRow - 1, currRow - 1, 1, 2), sheet);

        return currRow;
    }

    private void insertItem(ColItem item, HSSFRow row) {
        insertCell(1, row, item.getArean());
        insertCell(2, row, item.getCompNum());
        insertCols(3, item.getEachCol(), row);
    }

    private void insertCols(int startCol, double[] cols, HSSFRow row) {
        for (double col : cols) {
            insertCell(startCol++, row, col);
        }
    }

    private void insertCell(int col, HSSFRow row,String value) {
        HSSFCell cell = row.createCell(col);
        cell.setCellValue(value);
    }

    private void insertCell(int col, HSSFRow row, double value) {
        HSSFCell cell = row.createCell(col);
        cell.setCellValue(value);
    }

}
