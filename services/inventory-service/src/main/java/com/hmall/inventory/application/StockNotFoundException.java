package com.hmall.inventory.application;

/**
 * 库存记录不存在，映射为 404。
 */
public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String message) {
        super(message);
    }
}
