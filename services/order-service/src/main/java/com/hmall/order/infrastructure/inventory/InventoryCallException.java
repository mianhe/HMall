package com.hmall.order.infrastructure.inventory;

/**
 * 调用 Inventory 服务返回 4xx 时抛出，由 OrderApplicationService 捕获并转为 OrderBadRequestException。
 */
public class InventoryCallException extends RuntimeException {

    public InventoryCallException(String message) {
        super(message);
    }

    public InventoryCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
