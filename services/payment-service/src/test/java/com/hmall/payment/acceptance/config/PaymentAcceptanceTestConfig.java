package com.hmall.payment.acceptance.config;

import com.hmall.payment.acceptance.PaymentCallbackStepDefinitions;
import com.hmall.payment.acceptance.PaymentCreateStepDefinitions;
import com.hmall.payment.acceptance.PaymentExpireStepDefinitions;
import com.hmall.payment.acceptance.PaymentQueryStepDefinitions;
import com.hmall.payment.acceptance.PaymentRefundStepDefinitions;
import com.hmall.payment.acceptance.PaymentSmokeStepDefinitions;
import com.hmall.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PaymentAcceptanceTestConfig {

    @Bean
    @Primary
    public PaymentSmokeStepDefinitions paymentSmokeStepDefinitions() {
        return new PaymentSmokeStepDefinitions();
    }

    @Bean
    @Primary
    public PaymentCreateStepDefinitions paymentCreateStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastPaymentContext lastPaymentContext,
            PaymentJpaRepository paymentJpaRepository,
            PaymentEventCapture eventCapture) {
        return new PaymentCreateStepDefinitions(restTemplate, lastResponseContext, lastPaymentContext,
            paymentJpaRepository, eventCapture);
    }

    @Bean
    @Primary
    public PaymentCallbackStepDefinitions paymentCallbackStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastPaymentContext lastPaymentContext,
            PaymentEventCapture eventCapture) {
        return new PaymentCallbackStepDefinitions(restTemplate, lastResponseContext, lastPaymentContext, eventCapture);
    }

    @Bean
    @Primary
    public PaymentExpireStepDefinitions paymentExpireStepDefinitions(
            TestRestTemplate restTemplate,
            LastPaymentContext lastPaymentContext,
            PaymentEventCapture eventCapture) {
        return new PaymentExpireStepDefinitions(restTemplate, lastPaymentContext, eventCapture);
    }

    @Bean
    @Primary
    public PaymentRefundStepDefinitions paymentRefundStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext) {
        return new PaymentRefundStepDefinitions(restTemplate, lastResponseContext);
    }

    @Bean
    @Primary
    public PaymentQueryStepDefinitions paymentQueryStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastPaymentContext lastPaymentContext) {
        return new PaymentQueryStepDefinitions(restTemplate, lastResponseContext, lastPaymentContext);
    }

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    public LastPaymentContext lastPaymentContext() {
        return new LastPaymentContext();
    }

    @Bean
    public PaymentEventCapture paymentEventCapture() {
        return new PaymentEventCapture();
    }
}
