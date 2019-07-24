package com.onek.calculate.filter;

import com.onek.calculate.entity.IDiscount;
import com.onek.calculate.entity.IProduct;
import com.onek.calculate.entity.Package;
import com.onek.calculate.entity.Product;
import com.onek.util.stock.RedisStockUtil;
import org.hyrdpf.util.LogUtil;

/**
 * 库存过滤器
 */
public class StoreFilter extends BaseFilter {

    @Override
    protected boolean isFilter(IDiscount iDiscount, IProduct product) {
        long discountNo = iDiscount.getDiscountNo();

        if (product instanceof Package) {
            Package p = (Package) product;

            if (p.getExpireFlag() == 0) {
                int store;
                for (Product product1 : p.getPacageProdList()) {
                    store = RedisStockUtil.getActStockBySkuAndActno(product1.getSKU(), discountNo);

                    if (store < product1.getNums()) {
                        LogUtil.getDefaultLogger().info("This actStore is Not Enough, store is " + store
                                + ", sku is " + product1.getSKU() + " AND actNo is " + discountNo);

                        return true;
                    }
                }
            }

        } else {
            int store = RedisStockUtil.getActStockBySkuAndActno(product.getSKU(), discountNo);

            if (store <= 0) {
                LogUtil.getDefaultLogger().info("This actStore is Empty"
                        + ", sku is " + product.getSKU() + " AND actNo is " + discountNo);

                return true;
            }
        }

        return false;
    }
}
