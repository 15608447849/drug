package com.onek.report.col;

import com.onek.report.core.IDoubleCal;
import com.onek.report.core.IRowData;
import com.onek.report.init.ColInfo;
import util.ArrayUtil;

import java.util.List;

public class ColTotal implements IRowData, IDoubleCal {
    private List<ColGroup> colGroups;
    private boolean isAcc;

    public ColTotal(List<ColGroup> groups, boolean isAcc) {
        this.colGroups = groups;
        this.isAcc = isAcc;
    }

    @Override
    public double[] getEachCol() {
        if (this.isAcc) {
            if (colGroups != null && !colGroups.isEmpty()) {
                return this.colGroups.get(colGroups.size() - 1).getEachCol();
            } else {
                return null;
            }
        }

        double[] result = null;
        double[] dataCol;
//        double totalAvg = .0;
        for (ColGroup data : colGroups) {
            dataCol = data.getEachCol();
//            totalAvg = addDouble(totalAvg, dataCol[ColInfo.COL_NUM_ORDERNUM_AVG]);

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
//            result[ColInfo.COL_NUM_ORDERNUM_AVG] = divDouble(totalAvg, colGroups.size());
//        }

        return result;
    }

    public List<ColGroup> getColGroups() {
        return colGroups;
    }
}
