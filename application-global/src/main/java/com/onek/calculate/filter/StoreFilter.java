package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import com.onek.util.stock.RedisStockUtil;
import org.hyrdpf.util.LogUtil;

/**
 * 库存过滤器
 */
public class StoreFilter extends BaseFilter {

    @Override
    protected boolean isFilter(IDiscount iDiscount, IProduct product) {
        int store = RedisStockUtil.getActStockBySkuAndActno(product.getSKU(), iDiscount.getDiscountNo());

        LogUtil.getDefaultLogger().info("Current actStore is " + store
                + ", sku is " + product.getSKU() + " AND actNo is " + iDiscount.getDiscountNo());

        return store <= 0;
    }
}
