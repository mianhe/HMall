package com.hmall.order.application.port;

public interface CouponLifecyclePort {

    void lock(Long couponId, Long orderId);

    void redeem(Long couponId);

    void release(Long couponId);
}
