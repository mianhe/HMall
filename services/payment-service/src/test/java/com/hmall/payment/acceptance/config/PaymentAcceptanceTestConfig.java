package com.hmall.payment.acceptance.config;

import com.hmall.payment.acceptance.PaymentCallbackStepDefinitions;
import com.hmall.payment.acceptance.PaymentCreateStepDefinitions;
import com.hmall.payment.acceptance.PaymentExpireStepDefinitions;
import com.hmall.payment.acceptance.PaymentQueryStepDefinitions;
import com.hmall.payment.acceptance.PaymentRefundStepDefinitions;
import com.hmall.payment.acceptance.PaymentSettingsStepDefinitions;
import com.hmall.payment.acceptance.PaymentSmokeStepDefinitions;
import com.hmall.payment.application.port.PaymentDomainEventPublisher;
import com.hmall.payment.infrastructure.config.PaymentProperties;
import com.hmall.payment.infrastructure.config.PaymentSettingsInitializer;
import com.hmall.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PaymentAcceptanceTestConfig {

    @Bean
    public PaymentEventCapture paymentEventCapture() {
        return new PaymentEventCapture();
    }

    @Bean
    @Primary
    public PaymentDomainEventPublisher testEventPublisher(PaymentEventCapture capture) {
        return capture;
    }

    @Bean
    @Primary
    public PaymentSmokeStepDefinitions paymentSmokeStepDefinitions(TestRestTemplate restTemplate) {
        return new PaymentSmokeStepDefinitions(restTemplate);
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
    @Primary
    public PaymentSettingsStepDefinitions paymentSettingsStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastPaymentContext lastPaymentContext,
            PaymentProperties paymentProperties,
            PaymentSettingsInitializer settingsInitializer) {
        return new PaymentSettingsStepDefinitions(restTemplate, lastResponseContext, lastPaymentContext,
                paymentProperties, settingsInitializer);
    }

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    public LastPaymentContext lastPaymentContext() {
        return new LastPaymentContext();
    }
}
