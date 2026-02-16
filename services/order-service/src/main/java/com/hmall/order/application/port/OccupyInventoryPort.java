package com.hmall.order.application.port;

import java.util.List;

/** 同步占用库存。Order 创建订单时调用。失败则抛出异常。 */
public interface OccupyInventoryPort {

    /**
     * 占用库存。任一 skuId 库存不足时抛出异常。
     *
     * @param orderId 订单 ID
     * @param items   明细 [{skuId, quantity}]
     */
    void occupy(Long orderId, List<ItemQuantity> items);

    record ItemQuantity(long skuId, int quantity) {}
}
