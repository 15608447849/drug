package com.onek.util.sqltransfer;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: leeping
 * @Date: 2019/5/20 10:40
 */
public class SqlRemoteReq {
    public String[] sqlArr;
    public String sql;
    public List<Object[]> params;
    public int length;

   public Object[] objects;

    public SqlRemoteReq(String sql, Object[] objects) {
        this.sql = sql;
        this.objects = objects;
    }

    public SqlRemoteReq(String[] sqlArr, List<Object[]> params) {
        this.sqlArr = sqlArr;
        this.params = params;
    }

    public SqlRemoteReq(String sql, List<Object[]> params, int length) {
        this.sql = sql;
        this.params = params;
        this.length = length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object[] objs : params){
            Arrays.toString(objs);
            sb.append(objs+"\n");
        }
        return "执行sql:"+sql+" 参数列表:"+ sb.toString() +" ,length="+length;
    }
}
