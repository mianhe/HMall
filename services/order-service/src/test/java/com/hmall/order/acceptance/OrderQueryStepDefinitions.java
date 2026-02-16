package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.LastOrderContext;
import com.hmall.order.acceptance.config.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderQueryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final UserStubStepDefinitions userStub;
    private final LastResponseContext lastResponseContext;
    private final LastOrderContext lastOrderContext;

    private ResponseEntity<OrderApiDto.CreateResponse> lastGetOrderResponse;
    private ResponseEntity<OrderListResponse> lastListOrdersResponse;

    public OrderQueryStepDefinitions(
            TestRestTemplate restTemplate,
            UserStubStepDefinitions userStub,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext) {
        this.restTemplate = restTemplate;
        this.userStub = userStub;
        this.lastResponseContext = lastResponseContext;
        this.lastOrderContext = lastOrderContext;
    }

    @When("按 orderId 查询该订单")
    public void 按orderId查询该订单() {
        Long orderId = lastOrderContext.getLastOrderId();
        assertThat(orderId).isNotNull();
        executeGetOrder(orderId);
    }

    @When("按 orderId 查询不存在的订单 {long}")
    public void 按orderId查询不存在的订单(long orderId) {
        executeGetOrder(orderId);
    }

    private void executeGetOrder(Long orderId) {
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
        try {
            ResponseEntity<OrderApiDto.CreateResponse> resp = restTemplate.exchange(
                    "/api/orders/" + orderId,
                    HttpMethod.GET,
                    null,
                    OrderApiDto.CreateResponse.class
            );
            lastResponseContext.setLastStatusCode(resp.getStatusCode().value());
            lastGetOrderResponse = resp;
        } catch (RestClientResponseException e) {
            lastResponseContext.setLastStatusCode(e.getStatusCode().value());
            lastGetOrderResponse = null;
        }
    }

    @Then("应返回订单详情")
    public void 应返回订单详情() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
        assertThat(lastGetOrderResponse).isNotNull();
        assertThat(lastGetOrderResponse.getBody()).isNotNull();
    }

    @And("订单详情包含 orderId、status、items、shippingAddress")
    public void 订单详情包含orderIdStatusItemsShippingAddress() {
        OrderApiDto.CreateResponse body = lastGetOrderResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.orderId).isNotNull();
        assertThat(body.status).isNotNull();
        assertThat(body.items).isNotNull();
        assertThat(body.shippingAddress).isNotNull();
    }

    @And("订单 status 为 PENDING_PAYMENT")
    public void 订单status为PENDING_PAYMENT() {
        assertThat(lastGetOrderResponse.getBody()).isNotNull();
        assertThat(lastGetOrderResponse.getBody().status).isEqualTo("PENDING_PAYMENT");
    }

    @When("按 userId 查询 {string} 的订单列表 第 {int} 页 每页 {int} 条")
    public void 按userId查询订单列表(String username, int pageOneBased, int size) {
        Long userId = userStub.getUserId(username);
        assertThat(userId).isNotNull();
        int page = pageOneBased - 1;
        executeListOrders(userId, page, size);
    }

    private void executeListOrders(Long userId, int page, int size) {
        restTemplate.getRestTemplate().setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
        });
        String url = "/api/orders?userId=" + userId + "&page=" + page + "&size=" + size;
        ResponseEntity<OrderListResponse> resp = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                OrderListResponse.class
        );
        lastResponseContext.setLastStatusCode(resp.getStatusCode().value());
        lastListOrdersResponse = resp;
    }

    @Then("应返回订单列表")
    public void 应返回订单列表() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
        assertThat(lastListOrdersResponse).isNotNull();
        assertThat(lastListOrdersResponse.getBody()).isNotNull();
    }

    @And("订单列表包含 {int} 条订单")
    public void 订单列表包含条订单(int count) {
        assertThat(lastListOrdersResponse.getBody()).isNotNull();
        assertThat(lastListOrdersResponse.getBody().content).hasSize(count);
    }

    @And("列表中的订单包含 orderId、status")
    public void 列表中的订单包含orderIdStatus() {
        assertThat(lastListOrdersResponse.getBody()).isNotNull();
        List<OrderApiDto.CreateResponse> content = lastListOrdersResponse.getBody().content;
        for (OrderApiDto.CreateResponse order : content) {
            assertThat(order.orderId).isNotNull();
            assertThat(order.status).isNotNull();
        }
    }

    public static class OrderListResponse {
        public List<OrderApiDto.CreateResponse> content;
        public Long totalElements;
    }
}
