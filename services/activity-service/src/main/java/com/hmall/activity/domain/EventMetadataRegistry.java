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
        // Order (智能运营 Step 1: processRoles)
        register(EventMetadata.normal("OrderCreated", "Order", "订单创建", EventOrigin.DOMAIN, Map.of("trading", "MILESTONE")));
        register(EventMetadata.compensation("OrderCancelled", "Order", "订单取消", "OrderCreated", EventOrigin.DOMAIN, Map.of("trading", "MILESTONE")));
        register(EventMetadata.normal("OrderCompleted", "Order", "订单完成", EventOrigin.DOMAIN,
                Map.of("trading", "MILESTONE", "user_development", "PROGRESSION", "product_ops", "PROGRESSION")));

        // Inventory
        register(EventMetadata.normal("StockReserved", "Inventory", "库存锁定", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.compensation("StockReleased", "Inventory", "库存释放", "StockReserved", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));

        // Payment
        register(EventMetadata.normal("PaymentCompleted", "Payment", "支付成功", EventOrigin.DOMAIN, Map.of("trading", "MILESTONE")));
        register(EventMetadata.exception("PaymentFailed", "Payment", "支付失败", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.exception("PaymentExpired", "Payment", "支付超时", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));

        // Fulfillment
        register(EventMetadata.normal("FulfillmentOrderCreated", "Fulfillment", "履约单创建", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.normal("FulfillmentOrderAllocated", "Fulfillment", "开始配货", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.normal("FulfillmentShipped", "Fulfillment", "已发货", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.normal("FulfillmentDelivered", "Fulfillment", "已签收", EventOrigin.DOMAIN, Map.of("trading", "MILESTONE")));
        register(EventMetadata.normal("ServiceActivated", "Fulfillment", "服务已激活", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
        register(EventMetadata.normal("EngravingCompleted", "Fulfillment", "镭雕已完成", EventOrigin.DOMAIN, Map.of("trading", "PROGRESSION")));
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
