package com.hmall.order.acceptance.config;

import com.hmall.order.acceptance.OrderEventCapture;
import com.hmall.order.application.port.CancelFulfillmentPort;
import com.hmall.order.application.port.CreateFulfillmentPort;
import com.hmall.order.application.port.OccupyInventoryPort;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
import com.hmall.order.application.port.RefundPaymentPort;
import com.hmall.order.application.port.ReleaseInventoryPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
    public RefundPaymentPort refundPaymentPort() {
        return orderId -> {};
    }

    @Bean
    @Primary
    public OrderOutboundEventPublisher orderOutboundEventPublisher(OrderEventCapture orderEventCapture) {
        return orderEventCapture;
    }
}
