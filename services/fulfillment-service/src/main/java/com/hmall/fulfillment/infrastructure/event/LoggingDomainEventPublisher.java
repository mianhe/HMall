package com.hmall.fulfillment.infrastructure.event;

import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.EngravingCompleted;
import com.hmall.fulfillment.domain.FulfillmentDelivered;
import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import com.hmall.fulfillment.domain.FulfillmentOrderCreated;
import com.hmall.fulfillment.domain.FulfillmentShipped;
import com.hmall.fulfillment.domain.ServiceActivated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认实现：仅记录日志。Kafka 实现就绪后由其 @Primary 覆盖。
 */
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(FulfillmentOrderCreated event) {
        log.info("领域事件 FulfillmentOrderCreated: orderId={}, fulfillmentOrderIds={}",
                event.orderId(), event.fulfillmentOrderIds());
    }

    @Override
    public void publish(FulfillmentOrderAllocated event) {
        log.info("领域事件 FulfillmentOrderAllocated: orderId={}, fulfillmentOrderId={}",
                event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(FulfillmentShipped event) {
        log.info("领域事件 FulfillmentShipped: orderId={}, fulfillmentOrderId={}",
                event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(FulfillmentDelivered event) {
        log.info("领域事件 FulfillmentDelivered: orderId={}, fulfillmentOrderId={}",
                event.orderId(), event.fulfillmentOrderId());
    }

    @Override
    public void publish(ServiceActivated event) {
        log.info("领域事件 ServiceActivated: orderId={}, fulfillmentOrderId={}, serviceSkuId={}",
            event.orderId(), event.fulfillmentOrderId(), event.serviceSkuId());
    }

    @Override
    public void publish(EngravingCompleted event) {
        log.info("领域事件 EngravingCompleted: orderId={}, fulfillmentOrderId={}",
            event.orderId(), event.fulfillmentOrderId());
    }
}
