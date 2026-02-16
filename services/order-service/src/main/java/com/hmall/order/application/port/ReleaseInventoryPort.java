package com.hmall.order.application.port;

/** 同步释放库存。Order 取消订单时调用。 */
public interface ReleaseInventoryPort {

    void release(Long orderId);
}
