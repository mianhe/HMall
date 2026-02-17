package com.hmall.order.infrastructure.inventory;

/**
 * 调用 Inventory 服务时连接失败（服务未启动或网络不可达）时抛出，映射为 HTTP 503。
 */
public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(String message) {
        super(message);
    }

    public InventoryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
