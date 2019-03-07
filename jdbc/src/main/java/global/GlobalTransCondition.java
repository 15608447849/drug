package global;

import java.util.List;

public class GlobalTransCondition {

    private int sharding;

    private String[] nativeSQL;

    private  List<Object[]> params;

    public int getSharding() {
        return sharding;
    }

    public void setSharding(int sharding) {
        this.sharding = sharding;
    }

    public String[] getNativeSQL() {
        return nativeSQL;
    }

    public void setNativeSQL(String[] nativeSQL) {
        this.nativeSQL = nativeSQL;
    }

    public List<Object[]> getParams() {
        return params;
    }

    public void setParams(List<Object[]> params) {
        this.params = params;
    }
}
