package com.onek.report.col;

import com.onek.report.core.IDoubleCal;
import com.onek.report.core.IRowData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;

public class ColTotal implements IRowData, IDoubleCal {
    private List<ColGroup> colGroups;

    public ColTotal(List<ColGroup> groups) {
        this.colGroups = groups;
    }

    @Override
    public double[] getEachCol() {
        double[] result = null;
        double[] dataCol;

        double totalAvg = .0;
        for (ColGroup data : colGroups) {
            dataCol = data.getEachCol();
            totalAvg = addDouble(totalAvg, dataCol[20]);

            if (result == null) {
                result = dataCol;
            } else {
                for (int i = 0; i < result.length; i++) {
                    if (i >= result.length - 3) {
                        switch (i) {
                            case 18:
                                result[i] = Math.max(result[i], dataCol[i]);
                                break;
                            case 19:
                                result[i] = Math.min(result[i], dataCol[i]);
                                break;
                        }
                    } else {
                        result[i] = addDouble(result[i], dataCol[i]);
                    }
                }
            }
        }

        result[20] = divDouble(totalAvg, colGroups.size());

        return result;
    }

    public List<ColGroup> getColGroups() {
        return colGroups;
    }
}
