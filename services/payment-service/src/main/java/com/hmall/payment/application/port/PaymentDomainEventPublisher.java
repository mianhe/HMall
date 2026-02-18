package com.hmall.payment.application.port;

import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;

/** 支付领域事件发布端口。由基础设施实现（如 Spring 事件或 Kafka）。 */
public interface PaymentDomainEventPublisher {

    void publish(PaymentCompletedEvent event);

    void publish(PaymentFailedEvent event);

    void publish(PaymentExpiredEvent event);
}
