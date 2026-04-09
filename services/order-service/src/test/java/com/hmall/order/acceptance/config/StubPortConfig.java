package com.hmall.order.acceptance.config;

import com.hmall.order.acceptance.OrderEventCapture;
import com.hmall.order.application.port.*;
import com.hmall.order.domain.DiscountDetail;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;

@TestConfiguration
public class StubPortConfig {

    @Bean
    public EventInvocationRecorder eventInvocationRecorder() {
        return new EventInvocationRecorder();
    }

    @Bean
    public OrderEventCapture orderEventCapture() {
        return new OrderEventCapture();
    }

    @Bean
    @Primary
    public CreateFulfillmentPort createFulfillmentStub(EventInvocationRecorder recorder) {
        return recorder::recordCreateFulfillment;
    }

    @Bean
    @Primary
    public CancelFulfillmentPort cancelFulfillmentStub(EventInvocationRecorder recorder) {
        return recorder::recordCancelFulfillment;
    }

    @Bean
    @Primary
    public OccupyInventoryStub occupyInventoryStub() {
        return new OccupyInventoryStub();
    }

    @Bean
    @Primary
    public ReleaseInventoryPort releaseInventoryPort() {
        return orderId -> {};
    }

    @Bean
    @Primary
    public CreatePaymentPort createPaymentPort() {
        return (orderId, amountCents) -> {};
    }

    @Bean
    @Primary
    public RefundPaymentPort refundPaymentPort() {
        return orderId -> {};
    }

    @Bean
    @Primary
    public OrderOutboundEventPublisher orderOutboundEventPublisher(OrderEventCapture orderEventCapture) {
        return orderEventCapture;
    }

    @Bean
    @Primary
    public PromotionPricePort promotionPricePort() {
        return (items, userId, couponId) -> {
            long original = items == null ? 0L : items.stream()
                .mapToLong(i -> i.unitPriceCents() * i.quantity())
                .sum();
            List<PromotionPricePort.LineDiscount> lineDiscounts = items == null ? List.of() : items.stream()
                .map(i -> new PromotionPricePort.LineDiscount(i.skuId(), List.<DiscountDetail>of()))
                .toList();
            return new PromotionPricePort.PriceResult(original, 0L, original, lineDiscounts);
        };
    }

    @Bean
    @Primary
    public CouponLifecyclePort couponLifecyclePort() {
        return new CouponLifecyclePort() {
            @Override
            public void lock(Long couponId, Long orderId) {}

            @Override
            public void redeem(Long couponId) {}

            @Override
            public void release(Long couponId) {}
        };
    }
}
