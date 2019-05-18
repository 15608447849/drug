package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import com.onek.util.stock.RedisStockUtil;

/**
 * 库存过滤器
 */
public class StoreFilter extends BaseFilter {

    @Override
    protected boolean isFilter(IDiscount iDiscount, IProduct product) {
        int store = RedisStockUtil.getActStockBySkuAndActno(product.getSKU(), iDiscount.getDiscountNo());

        return store <= 0;
    }
}
