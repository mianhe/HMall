package com.hmall.inventory.application;

/**
 * 业务校验失败（如库存不足、参数非法），映射为 400。
 */
public class InventoryBadRequestException extends RuntimeException {

    public InventoryBadRequestException(String message) {
        super(message);
    }
}
