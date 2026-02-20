package com.hmall.order.acceptance.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hmall.order.acceptance.CatalogStubStepDefinitions;
import com.hmall.order.acceptance.InventoryStubStepDefinitions;
import com.hmall.order.acceptance.OrderCancelStepDefinitions;
import com.hmall.order.acceptance.OrderSmokeStepDefinitions;
import com.hmall.order.acceptance.OrderCreateStepDefinitions;
import com.hmall.order.acceptance.OrderEventCapture;
import com.hmall.order.acceptance.OrderEventsStepDefinitions;
import com.hmall.order.acceptance.OrderQueryStepDefinitions;
import com.hmall.order.acceptance.UserStubStepDefinitions;
import com.hmall.order.application.OrderEventService;
import com.hmall.order.domain.OrderRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration
@Import({ CatalogWireMockConfig.class, UserWireMockConfig.class, StubPortConfig.class })
public class OrderAcceptanceTestConfig {

    @Bean
    @Primary
    public OrderSmokeStepDefinitions orderSmokeStepDefinitions(TestRestTemplate restTemplate) {
        return new OrderSmokeStepDefinitions(restTemplate);
    }

    @Bean
    @Primary
    public OrderQueryStepDefinitions orderQueryStepDefinitions(
            TestRestTemplate restTemplate,
            UserStubStepDefinitions userStub,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext) {
        return new OrderQueryStepDefinitions(restTemplate, userStub, lastResponseContext, lastOrderContext);
    }

    @Bean
    @Primary
    public OrderEventsStepDefinitions orderEventsStepDefinitions(
            LastOrderContext lastOrderContext,
            EventInvocationRecorder eventInvocationRecorder,
            OrderEventCapture orderEventCapture,
            OrderEventService orderEventService,
            TestRestTemplate restTemplate,
            OrderRepository orderRepository) {
        return new OrderEventsStepDefinitions(lastOrderContext, eventInvocationRecorder,
                orderEventCapture, orderEventService, restTemplate, orderRepository);
    }

    @Bean
    @Primary
    public OrderCreateStepDefinitions orderCreateStepDefinitions(
            TestRestTemplate restTemplate,
            UserStubStepDefinitions userStub,
            CatalogStubStepDefinitions catalogStub,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext,
            OrderEventCapture orderEventCapture) {
        return new OrderCreateStepDefinitions(restTemplate, userStub, catalogStub, lastResponseContext, lastOrderContext, orderEventCapture);
    }

    @Bean
    @Primary
    public InventoryStubStepDefinitions inventoryStubStepDefinitions(OccupyInventoryStub occupyInventoryStub) {
        return new InventoryStubStepDefinitions(occupyInventoryStub);
    }

    @Bean
    @Primary
    public OrderCancelStepDefinitions orderCancelStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext,
            OrderRepository orderRepository,
            OrderEventCapture orderEventCapture) {
        return new OrderCancelStepDefinitions(restTemplate, lastResponseContext, lastOrderContext, orderRepository, orderEventCapture);
    }

    @Bean
    @Primary
    public UserStubStepDefinitions userStubStepDefinitions(WireMockServer userWireMock) {
        return new UserStubStepDefinitions(userWireMock);
    }

    @Bean
    @Primary
    public CatalogStubStepDefinitions catalogStubStepDefinitions(WireMockServer catalogWireMock) {
        return new CatalogStubStepDefinitions(catalogWireMock);
    }

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    public LastOrderContext lastOrderContext() {
        return new LastOrderContext();
    }
}
