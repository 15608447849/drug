package com.onek.report.col;

import com.onek.report.core.IDoubleCal;
import com.onek.report.core.IRowData;
import com.onek.report.init.ColInfo;
import com.onek.util.area.AreaUtil;
import constant.DSMConst;
import dao.BaseDAO;
import util.ArrayUtil;

import java.util.*;

public class ColGroup implements IRowData, IDoubleCal {
    private static final Map<Long, String> NAMESTORE = new HashMap<>();

    private String dateLabel;
    private String date;
    private String groupLable;
    private List<ColItem> colItems;


    public ColGroup() {
        this.colItems = new ArrayList<>();
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public List<ColItem> getColItems() {
        this.colItems.sort(new Comparator<ColItem>() {
            @Override
            public int compare(ColItem o1, ColItem o2) {
                return (int) (o1.getAreac() - o2.getAreac());
            }
        });

        return colItems;
    }

    public String getGroupLabel() {
        if (groupLable != null) {
            return groupLable;
        }

        if (colItems.isEmpty()) {
            return groupLable = "";
        }

        long parent = AreaUtil.getParent(colItems.get(0).getAreac());

        String name = NAMESTORE.get(parent);

        if (name == null) {
            name = getArean(parent);
            NAMESTORE.put(parent, name);
        }

        return groupLable = name;
    }

    private String getArean(long areac) {
        if (areac == 0) {
            return "";
        }

        String sql = " SELECT arean "
                + " FROM {{?" + DSMConst.TB_AREA_PCA + "}} "
                + " WHERE cstatus&1 = 0 AND areac = ? ";

        List<Object[]> queryResult = BaseDAO.getBaseDAO().queryNative(sql, areac);

        if (queryResult.isEmpty()) {
            return "";
        }

        return queryResult.get(0)[0].toString();
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public double[] getEachCol() {
        double[] result = null;
        double[] dataCol;

        double totalAvg = .0;
        for (ColItem data : colItems) {
            dataCol = data.getEachCol();
            totalAvg = addDouble(totalAvg, dataCol[ColInfo.COL_NUM_ORDERNUM_AVG]);

            if (result == null) {
                result = dataCol;
            } else {
                for (int i = 0; i < result.length; i++) {
                    switch (ColInfo.COL_STATUS[i]) {
                        case 0:
                            result[i] = addDouble(result[i], dataCol[i]);
                            break;
                        case 1:
                            result[i] = Math.max(result[i], dataCol[i]);
                            break;
                        case 2:
                            result[i] = Math.min(result[i], dataCol[i]);
                            break;
                        case 3:
                            break;
                        case 4:
                            if (i == 6) {
                                result[i] = divDouble(result[4] * 100, result[3]);
                            } else if (i == 7) {
                                result[i] = divDouble(result[5] * 100, result[3]);
                            } else if (i == 9) {
                                result[i] = divDouble(result[3] * 100, result[8]);
                            } else if (i == 15) {
                                result[i] = divDouble(result[14] * 100, result[13]);
                            } else if (i == 17) {
                                result[i] = divDouble(result[13] * 100, result[16]);
                            } else if (i == 20) {
                                result[i] = divDouble(result[16], result[8]);
                            }

                            break;
                    }
                }
            }
        }

//        if (!ArrayUtil.isEmpty(result)) {
//            result[ColInfo.COL_NUM_ORDERNUM_AVG] = divDouble(totalAvg, colItems.size());
//        }

        return result;
    }
}
