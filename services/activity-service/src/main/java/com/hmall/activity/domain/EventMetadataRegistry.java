package com.hmall.activity.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 事件元数据注册表：集中管理所有已知事件类型的业务语义。
 * 新增事件类型时只需在此处注册一行，前端无需任何改动。
 */
public class EventMetadataRegistry {

    private static final Map<String, EventMetadata> REGISTRY = new LinkedHashMap<>();

    static {
        // Order
        register(EventMetadata.normal("OrderCreated", "Order", "订单创建"));
        register(EventMetadata.compensation("OrderCancelled", "Order", "订单取消", "OrderCreated"));
        register(EventMetadata.normal("OrderCompleted", "Order", "订单完成"));

        // Inventory
        register(EventMetadata.normal("StockReserved", "Inventory", "库存锁定"));
        register(EventMetadata.compensation("StockReleased", "Inventory", "库存释放", "StockReserved"));

        // Payment
        register(EventMetadata.normal("PaymentCompleted", "Payment", "支付成功"));
        register(EventMetadata.exception("PaymentFailed", "Payment", "支付失败"));
        register(EventMetadata.exception("PaymentExpired", "Payment", "支付超时"));

        // Fulfillment
        register(EventMetadata.normal("FulfillmentOrderCreated", "Fulfillment", "履约单创建"));
        register(EventMetadata.normal("FulfillmentOrderAllocated", "Fulfillment", "开始配货"));
        register(EventMetadata.normal("FulfillmentShipped", "Fulfillment", "已发货"));
        register(EventMetadata.normal("FulfillmentDelivered", "Fulfillment", "已签收"));
        register(EventMetadata.normal("ServiceActivated", "Fulfillment", "服务已激活"));
    }

    private static void register(EventMetadata metadata) {
        REGISTRY.put(metadata.eventType(), metadata);
    }

    public static Optional<EventMetadata> find(String eventType) {
        return Optional.ofNullable(REGISTRY.get(eventType));
    }

    public static Collection<EventMetadata> all() {
        return REGISTRY.values();
    }
}
