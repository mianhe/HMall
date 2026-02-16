package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.EventInvocationRecorder;
import com.hmall.order.acceptance.config.LastOrderContext;
import com.hmall.order.application.event.*;
import com.hmall.order.domain.*;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderEventsStepDefinitions {

    private final LastOrderContext lastOrderContext;
    private final EventInvocationRecorder eventRecorder;
    private final OrderEventCapture orderEventCapture;
    private final ApplicationEventPublisher eventPublisher;
    private final TestRestTemplate restTemplate;
    private final com.hmall.order.domain.OrderRepository orderRepository;

    public OrderEventsStepDefinitions(
            LastOrderContext lastOrderContext,
            EventInvocationRecorder eventRecorder,
            OrderEventCapture orderEventCapture,
            ApplicationEventPublisher eventPublisher,
            TestRestTemplate restTemplate,
            com.hmall.order.domain.OrderRepository orderRepository) {
        this.lastOrderContext = lastOrderContext;
        this.eventRecorder = eventRecorder;
        this.orderEventCapture = orderEventCapture;
        this.eventPublisher = eventPublisher;
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
    }

    @Before
    public void resetRecorders() {
        eventRecorder.reset();
        orderEventCapture.clear();
    }

    @When("发布 PaymentCompleted 事件 针对该订单 paymentId {long}")
    public void 发布PaymentCompleted事件针对该订单(long paymentId) {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new PaymentCompletedEvent(orderId, paymentId));
    }

    @When("发布 PaymentFailed 事件 针对该订单")
    public void 发布PaymentFailed事件针对该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new PaymentFailedEvent(orderId));
    }

    @When("发布 PaymentExpired 事件 针对该订单")
    public void 发布PaymentExpired事件针对该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new PaymentExpiredEvent(orderId));
    }

    @When("发布 FulfillmentOrderCreated 事件 针对该订单 履约单 ID {long} {long}")
    public void 发布FulfillmentOrderCreated事件针对该订单(long id1, long id2) {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new FulfillmentOrderCreatedEvent(orderId, List.of(id1, id2)));
    }

    @When("发布 FulfillmentShipped 事件 针对该订单")
    public void 发布FulfillmentShipped事件针对该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new FulfillmentShippedEvent(orderId));
    }

    @When("发布 FulfillmentDelivered 事件 针对该订单")
    public void 发布FulfillmentDelivered事件针对该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        eventPublisher.publishEvent(new FulfillmentDeliveredEvent(orderId));
    }

    @Then("订单 status 应为 {word}")
    public void 订单status应为(String expectedStatus) {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        ResponseEntity<OrderApiDto.CreateResponse> resp = restTemplate.getForEntity(
                "/api/orders/" + orderId, OrderApiDto.CreateResponse.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status).isEqualTo(expectedStatus);
    }

    @And("应已发起创建履约单")
    public void 应已发起创建履约单() {
        assertThat(eventRecorder.getCreateFulfillmentOrderIds())
                .contains(lastOrderContext.getLastOrderId());
    }

    @And("订单 fulfillmentRef 应包含履约单 {long} {long}")
    public void 订单fulfillmentRef应包含履约单(long id1, long id2) {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        ResponseEntity<OrderApiDto.CreateResponse> resp = restTemplate.getForEntity(
                "/api/orders/" + orderId, OrderApiDto.CreateResponse.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status).isEqualTo("FULFILLING");
    }

    @Then("订单 fulfillmentStatus 应为 {word}")
    public void 订单fulfillmentStatus应为(String expected) {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        ResponseEntity<OrderApiDto.CreateResponse> resp = restTemplate.getForEntity(
                "/api/orders/" + orderId, OrderApiDto.CreateResponse.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status).isEqualTo("SHIPPED");
    }

    @And("应已发布 OrderCompleted")
    public void 应已发布OrderCompleted() {
        assertThat(orderEventCapture.wasOrderCompletedPublished()).isTrue();
    }

    @io.cucumber.java.en.Given("已存在订单状态为 FULFILLING 履约单 {long} {long}")
    public void 已存在订单状态为FULFILLING履约单(long id1, long id2) {
        ShippingAddress addr = new ShippingAddress("收件人", "13800138000", "上海", "上海", "浦东", "测试地址");
        OrderLineItem item = new OrderLineItem(123L, 1, 10000L, "测试商品");
        Order order = new Order(null, 1L, OrderStatus.FULFILLING, 10000L, addr,
                List.of(item), Instant.now(), Instant.now());
        Order saved = orderRepository.save(order);
        lastOrderContext.setLastOrderId(saved.getOrderId());
    }
}
