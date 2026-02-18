package com.hmall.payment.infrastructure;

import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import com.hmall.payment.application.port.PaymentDomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringPaymentDomainEventPublisher implements PaymentDomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringPaymentDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(PaymentCompletedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(PaymentFailedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    public void publish(PaymentExpiredEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
