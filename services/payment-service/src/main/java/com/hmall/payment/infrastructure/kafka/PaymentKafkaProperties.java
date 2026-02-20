package com.hmall.payment.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.kafka.topic")
public class PaymentKafkaProperties {

    private String paymentCompleted = "payment.completed";
    private String paymentFailed = "payment.failed";
    private String paymentExpired = "payment.expired";

    public String getPaymentCompleted() {
        return paymentCompleted;
    }

    public void setPaymentCompleted(String paymentCompleted) {
        this.paymentCompleted = paymentCompleted;
    }

    public String getPaymentFailed() {
        return paymentFailed;
    }

    public void setPaymentFailed(String paymentFailed) {
        this.paymentFailed = paymentFailed;
    }

    public String getPaymentExpired() {
        return paymentExpired;
    }

    public void setPaymentExpired(String paymentExpired) {
        this.paymentExpired = paymentExpired;
    }
}
