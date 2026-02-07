package com.hmall.catalog.application;

/**
 * 在非叶子类别下创建商品时抛出（仅叶子类别可挂 SPU）。
 */
public class NotLeafCategoryException extends RuntimeException {

    public NotLeafCategoryException(String message) {
        super(message);
    }
}
