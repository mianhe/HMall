package com.hmall.payment.acceptance.config;

import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 验收测试中捕获 Payment 发布的领域事件，供 Step Definitions 断言。 */
public class PaymentEventCapture implements ApplicationListener<ApplicationEvent> {

    private final List<PaymentCompletedEvent> completedEvents = new CopyOnWriteArrayList<>();
    private final List<PaymentFailedEvent> failedEvents = new CopyOnWriteArrayList<>();
    private final List<PaymentExpiredEvent> expiredEvents = new CopyOnWriteArrayList<>();

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> pa) {
            if (pa.getPayload() instanceof PaymentCompletedEvent e) {
                completedEvents.add(e);
            } else if (pa.getPayload() instanceof PaymentFailedEvent e) {
                failedEvents.add(e);
            } else if (pa.getPayload() instanceof PaymentExpiredEvent e) {
                expiredEvents.add(e);
            }
        }
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
