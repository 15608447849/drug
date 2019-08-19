package com.onek.express;

import java.util.HashMap;
import java.util.Map;

public class RouteServiceFQExpress extends BaseFQExpress {
    private int tracking_type = 1;
    private int method_type = 1;

    @Override
    protected String getService() {
        return "RouteService";
    }

    @Override
    protected Map<String, String> genRequstParams() {
        Map<String, String> params = new HashMap<>();
        params.put("method_type", String.valueOf(this.method_type));
        params.put("tracking_type", String.valueOf(this.tracking_type));
//        params.put("check_phoneNo", INNER_PROPERTIES.custid);

        return params;
    }

    @Override
    protected Map<String, String> genRequstParams(String code) {
        Map<String, String> params = genRequstParams();
        params.put("tracking_number", code);

        return params;
    }

    @Override
    public String getResult() {
        return super.getResult();
    }

    public String getResult(String code) {
        return super.getResult(code);
    }

}
