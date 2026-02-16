package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.LastOrderContext;
import com.hmall.order.acceptance.config.LastResponseContext;
import com.hmall.order.domain.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderCancelStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final LastOrderContext lastOrderContext;
    private final OrderRepository orderRepository;
    private final OrderEventCapture orderEventCapture;

    public OrderCancelStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext,
            OrderRepository orderRepository,
            OrderEventCapture orderEventCapture) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.lastOrderContext = lastOrderContext;
        this.orderRepository = orderRepository;
        this.orderEventCapture = orderEventCapture;
    }

    @When("取消订单")
    public void 取消订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        executeCancel(orderId);
    }

    @When("再次取消订单")
    public void 再次取消订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        executeCancel(orderId);
    }

    @When("取消订单 ID {long}")
    public void 取消订单ID(long orderId) {
        executeCancel(orderId);
    }

    @Given("已存在订单状态为 COMPLETED")
    public void 已存在订单状态为COMPLETED() {
        ShippingAddress addr = new ShippingAddress("收件人", "13800138000", "上海", "上海", "浦东", "测试地址");
        OrderLineItem item = new OrderLineItem(123L, 1, 10000L, "测试商品");  // skuId, qty, unitPriceCents, displayName
        Order order = new Order(null, 1L, OrderStatus.COMPLETED, 10000L, addr,
                List.of(item), Instant.now(), Instant.now());
        Order saved = orderRepository.save(order);
        lastOrderContext.setLastOrderId(saved.getOrderId());
    }

    @Given("已存在订单状态为 PAID")
    public void 已存在订单状态为PAID() {
        ShippingAddress addr = new ShippingAddress("收件人", "13800138000", "上海", "上海", "浦东", "测试地址");
        OrderLineItem item = new OrderLineItem(123L, 1, 10000L, "测试商品");
        Order order = new Order(null, 1L, OrderStatus.PAID, 10000L, addr,
                List.of(item), Instant.now(), Instant.now());
        Order saved = orderRepository.save(order);
        lastOrderContext.setLastOrderId(saved.getOrderId());
    }

    @When("取消该订单")
    public void 取消该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        executeCancel(orderId);
    }

    private void executeCancel(Long orderId) {
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false; // 不将 4xx/5xx 视为错误，由我们自行处理 status
            }
        });
        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/orders/" + orderId + "/cancel",
                HttpMethod.POST,
                null,
                Void.class
        );
        lastResponseContext.setLastStatusCode(resp.getStatusCode().value());
    }

    @Then("应取消成功")
    public void 应取消成功() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
    }

    @Then("应取消失败")
    public void 应取消失败() {
        int status = lastResponseContext.getLastStatusCode();
        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    @io.cucumber.java.en.And("应发布 OrderCancelled")
    public void 应发布OrderCancelled() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
        assertThat(orderEventCapture.wasOrderCancelledPublished()).isTrue();
    }
}
