package com.hmall.order.application.port;

import java.util.List;

/**
 * 同步创建履约单。Order 收到 PaymentCompleted 后调用。
 * 返回 Fulfillment 分配的 fulfillmentOrderIds。
 */
public interface CreateFulfillmentPort {

    List<Long> createFulfillment(Long orderId, List<ItemQuantity> items,
                                  ShippingAddress shippingAddress);

    record ItemQuantity(long skuId, int quantity, String itemType) {}

    record ShippingAddress(String recipientName, String phone,
                           String province, String city,
                           String district, String detail) {}
}
