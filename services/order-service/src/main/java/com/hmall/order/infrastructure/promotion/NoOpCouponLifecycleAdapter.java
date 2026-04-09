package com.hmall.order.infrastructure.promotion;

import com.hmall.order.application.port.CouponLifecyclePort;

public class NoOpCouponLifecycleAdapter implements CouponLifecyclePort {

    @Override
    public void lock(Long couponId, Long orderId) {}

    @Override
    public void redeem(Long couponId) {}

    @Override
    public void release(Long couponId) {}
}
