package com.hmall.fulfillment.acceptance.config;

import com.hmall.fulfillment.acceptance.DatabaseResetHook;
import com.hmall.fulfillment.acceptance.EventCapture;
import com.hmall.fulfillment.acceptance.FulfillmentAllocateStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentCancelStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentCompleteEngravingStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentCommonStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentCreateStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentDeliverStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentQueryStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentShipStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentSmokeStepDefinitions;
import com.hmall.fulfillment.acceptance.FulfillmentTestContext;
import com.hmall.fulfillment.domain.DomainEventPublisher;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import com.hmall.fulfillment.infrastructure.persistence.FulfillmentOrderJpaRepository;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AcceptanceTestConfig {

    @Bean
    public FulfillmentTestContext fulfillmentTestContext() {
        return new FulfillmentTestContext();
    }

    @Bean
    public EventCapture eventCapture() {
        return new EventCapture();
    }

    @Bean
    @Primary
    public DomainEventPublisher domainEventPublisher(EventCapture eventCapture) {
        return eventCapture;
    }

    @Bean
    @Primary
    public FulfillmentSmokeStepDefinitions fulfillmentSmokeStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context) {
        return new FulfillmentSmokeStepDefinitions(restTemplate, context);
    }

    @Bean
    @Primary
    public FulfillmentCommonStepDefinitions fulfillmentCommonStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            FulfillmentOrderRepository repository) {
        return new FulfillmentCommonStepDefinitions(restTemplate, context, repository);
    }

    @Bean
    @Primary
    public FulfillmentCreateStepDefinitions fulfillmentCreateStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            EventCapture eventCapture,
            FulfillmentOrderRepository repository) {
        return new FulfillmentCreateStepDefinitions(restTemplate, context, eventCapture, repository);
    }

    @Bean
    @Primary
    public FulfillmentAllocateStepDefinitions fulfillmentAllocateStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            EventCapture eventCapture) {
        return new FulfillmentAllocateStepDefinitions(restTemplate, context, eventCapture);
    }

    @Bean
    @Primary
    public FulfillmentShipStepDefinitions fulfillmentShipStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            EventCapture eventCapture) {
        return new FulfillmentShipStepDefinitions(restTemplate, context, eventCapture);
    }

    @Bean
    @Primary
    public FulfillmentDeliverStepDefinitions fulfillmentDeliverStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            EventCapture eventCapture) {
        return new FulfillmentDeliverStepDefinitions(restTemplate, context, eventCapture);
    }

    @Bean
    @Primary
    public FulfillmentCompleteEngravingStepDefinitions fulfillmentCompleteEngravingStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context,
            FulfillmentOrderRepository repository) {
        return new FulfillmentCompleteEngravingStepDefinitions(restTemplate, context, repository);
    }

    @Bean
    @Primary
    public FulfillmentCancelStepDefinitions fulfillmentCancelStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context) {
        return new FulfillmentCancelStepDefinitions(restTemplate, context);
    }

    @Bean
    @Primary
    public FulfillmentQueryStepDefinitions fulfillmentQueryStepDefinitions(
            TestRestTemplate restTemplate,
            FulfillmentTestContext context) {
        return new FulfillmentQueryStepDefinitions(restTemplate, context);
    }

    @Bean
    @Primary
    public DatabaseResetHook databaseResetHook(
            FulfillmentOrderJpaRepository fulfillmentOrderJpaRepository,
            EventCapture eventCapture,
            FulfillmentTestContext context) {
        return new DatabaseResetHook(fulfillmentOrderJpaRepository, eventCapture, context);
    }
}
