package com.hmall.payment.acceptance.config;

import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import com.hmall.payment.application.port.PaymentDomainEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 测试替身：实现 PaymentDomainEventPublisher，记录事件调用供断言。
 * 在测试中以 @Primary 覆盖生产实现。
 */
public class PaymentEventCapture implements PaymentDomainEventPublisher {

    private final List<PaymentCompletedEvent> completedEvents = new CopyOnWriteArrayList<>();
    private final List<PaymentFailedEvent> failedEvents = new CopyOnWriteArrayList<>();
    private final List<PaymentExpiredEvent> expiredEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(PaymentCompletedEvent event) {
        completedEvents.add(event);
    }

    @Override
    public void publish(PaymentFailedEvent event) {
        failedEvents.add(event);
    }

    @Override
    public void publish(PaymentExpiredEvent event) {
        expiredEvents.add(event);
    }

    public List<PaymentCompletedEvent> getCompletedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(completedEvents));
    }

    public List<PaymentFailedEvent> getFailedEvents() {
        return Collections.unmodifiableList(new ArrayList<>(failedEvents));
    }

    public List<PaymentExpiredEvent> getExpiredEvents() {
        return Collections.unmodifiableList(new ArrayList<>(expiredEvents));
    }

    public void reset() {
        completedEvents.clear();
        failedEvents.clear();
        expiredEvents.clear();
    }
}
