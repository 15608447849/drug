package com.onek.report.col;

import com.onek.report.core.IDoubleCal;
import com.onek.report.core.IRowData;
import com.onek.report.init.ColInfo;
import com.onek.util.IceRemoteUtil;
import com.onek.util.area.AreaUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            name = IceRemoteUtil.getArean(parent);
            NAMESTORE.put(parent, name);
        }

        return groupLable = name;
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
                    if (i >= result.length - 3) {
                        switch (i) {
                            case ColInfo.COL_NUM_ORDERNUM_MAX:
                                result[i] = Math.max(result[i], dataCol[i]);
                                break;
                            case ColInfo.COL_NUM_ORDERNUM_MIN:
                                result[i] = Math.min(result[i], dataCol[i]);
                                break;
                        }
                    } else {
                        result[i] = addDouble(result[i], dataCol[i]);
                    }
                }
            }
        }

        result[ColInfo.COL_NUM_ORDERNUM_AVG] = divDouble(totalAvg, colItems.size());

        return result;
    }
}
