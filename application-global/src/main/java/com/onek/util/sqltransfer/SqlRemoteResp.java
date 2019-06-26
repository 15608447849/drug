package com.onek.util.sqltransfer;

import org.hyrdpf.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/5/20 10:43
 */
public class SqlRemoteResp {
    public int res = 0;
    public List<String[]> lines;
    public int[] resArr;

    public void setLines(List<Object[]> lines){
        this.lines = new ArrayList<>();
        for (Object[] row : lines){
            String[] arr = new String[row.length];
            for (int i = 0; i<arr.length;i++) arr[i] = row[i]+"";
            this.lines.add(arr);
        }
    }

    public List<Object[]> getLines(){
        LogUtil.getDefaultLogger().info("远程调用返回,lines: "+ lines.size());
        List<Object[]> objects = new ArrayList<>();
        for (String[] row : lines){
            Object[] arr = new String[row.length];
            System.arraycopy(row, 0, arr, 0, arr.length);
            objects.add(arr);
        }
        return objects;
    }
}
