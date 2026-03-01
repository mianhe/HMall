package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.LastOrderContext;
import com.hmall.order.acceptance.config.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderCreateStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final UserStubStepDefinitions userStub;
    private final CatalogStubStepDefinitions catalogStub;
    private final LastResponseContext lastResponseContext;
    private final LastOrderContext lastOrderContext;
    private final OrderEventCapture orderEventCapture;

    private ResponseEntity<OrderApiDto.CreateResponse> lastCreateResponse;

    public OrderCreateStepDefinitions(
            TestRestTemplate restTemplate,
            UserStubStepDefinitions userStub,
            CatalogStubStepDefinitions catalogStub,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext,
            OrderEventCapture orderEventCapture) {
        this.restTemplate = restTemplate;
        this.userStub = userStub;
        this.catalogStub = catalogStub;
        this.lastResponseContext = lastResponseContext;
        this.lastOrderContext = lastOrderContext;
        this.orderEventCapture = orderEventCapture;
    }

    private Long getUserId(String username) {
        return userStub.getUserId(username);
    }

    private Long getSkuId(String productName) {
        return catalogStub.getSkuId(productName);
    }

    @When("用户 {string} 提交订单 收货地址 {string} {string} {string} {string} {string} {string} 购买 {string} 数量 {int}")
    public void 用户提交订单购买产品(String username, String recipient, String phone,
            String province, String city, String district, String detail,
            String productName, int quantity) {
        Long userId = getUserId(username);
        assertThat(userId).isNotNull();
        Long skuId = getSkuId(productName);
        assertThat(skuId).isNotNull();

        OrderApiDto.CreateRequest req = buildCreateRequest(userId, recipient, phone, province, city, district, detail);
        req.items = List.of(new OrderApiDto.LineItemCreate(skuId, quantity));

        executePostOrder(req);
    }

    @When("用户 {string} 提交混合订单 收货地址 {string} {string} {string} {string} {string} {string} 购买 {string} 数量 {int} 和服务 {string} 数量 {int}")
    public void 用户提交混合订单(
            String username, String recipient, String phone,
            String province, String city, String district, String detail,
            String physicalProductName, int physicalQuantity,
            String serviceProductName, int serviceQuantity) {
        Long userId = getUserId(username);
        assertThat(userId).isNotNull();
        Long physicalSkuId = getSkuId(physicalProductName);
        Long serviceSkuId = getSkuId(serviceProductName);
        assertThat(physicalSkuId).isNotNull();
        assertThat(serviceSkuId).isNotNull();

        OrderApiDto.CreateRequest req = buildCreateRequest(userId, recipient, phone, province, city, district, detail);
        req.items = List.of(
            new OrderApiDto.LineItemCreate(physicalSkuId, physicalQuantity),
            new OrderApiDto.LineItemCreate(serviceSkuId, serviceQuantity, physicalSkuId)
        );
        executePostOrder(req);
    }

    @When("用户 {string} 提交订单 收货地址 {string} {string} {string} {string} {string} {string} 购买 skuId {long} 数量 {int}")
    public void 用户提交订单购买SkuId(String username, String recipient, String phone,
            String province, String city, String district, String detail,
            long skuId, int quantity) {
        Long userId = getUserId(username);
        assertThat(userId).isNotNull();

        OrderApiDto.CreateRequest req = buildCreateRequest(userId, recipient, phone, province, city, district, detail);
        req.items = List.of(new OrderApiDto.LineItemCreate(skuId, quantity));

        executePostOrder(req);
    }

    @When("用户 {string} 提交订单 收货地址 {string} {string} {string} {string} {string} {string} 商品明细为空")
    public void 用户提交订单商品明细为空(String username, String recipient, String phone,
            String province, String city, String district, String detail) {
        Long userId = getUserId(username);
        assertThat(userId).isNotNull();

        OrderApiDto.CreateRequest req = buildCreateRequest(userId, recipient, phone, province, city, district, detail);
        req.items = List.of();

        executePostOrder(req);
    }

    @When("用户 {string} 提交订单 收货地址缺省 购买 {string} 数量 {int}")
    public void 用户提交订单收货地址缺省(String username, String productName, int quantity) {
        Long userId = getUserId(username);
        assertThat(userId).isNotNull();
        Long skuId = getSkuId(productName);
        assertThat(skuId).isNotNull();

        OrderApiDto.CreateRequest req = new OrderApiDto.CreateRequest();
        req.userId = userId;
        req.shippingAddress = new OrderApiDto.ShippingAddress("", "", "", "", "", "");
        req.items = List.of(new OrderApiDto.LineItemCreate(skuId, quantity));

        executePostOrder(req);
    }

    @When("用户 ID {long} 提交订单 收货地址 {string} {string} {string} {string} {string} {string} 购买 {string} 数量 {int}")
    public void 用户ID提交订单(long userId, String recipient, String phone,
            String province, String city, String district, String detail,
            String productName, int quantity) {
        Long skuId = getSkuId(productName);
        assertThat(skuId).isNotNull();

        OrderApiDto.CreateRequest req = buildCreateRequest(userId, recipient, phone, province, city, district, detail);
        req.items = List.of(new OrderApiDto.LineItemCreate(skuId, quantity));

        executePostOrder(req);
    }

    private void executePostOrder(OrderApiDto.CreateRequest body) {
        try {
            lastCreateResponse = restTemplate.exchange(
                "/api/orders",
                HttpMethod.POST,
                new HttpEntity<>(body, OrderApiDto.jsonHeaders()),
                OrderApiDto.CreateResponse.class
            );
            lastResponseContext.setLastStatusCode(lastCreateResponse.getStatusCode().value());
            if (lastCreateResponse.getBody() != null && lastCreateResponse.getBody().orderId != null) {
                lastOrderContext.setLastOrderId(lastCreateResponse.getBody().orderId);
            }
        } catch (RestClientResponseException e) {
            lastResponseContext.setLastStatusCode(e.getStatusCode().value());
            lastCreateResponse = null;
        }
    }

    private OrderApiDto.CreateRequest buildCreateRequest(Long userId, String recipient, String phone,
            String province, String city, String district, String detail) {
        OrderApiDto.CreateRequest req = new OrderApiDto.CreateRequest();
        req.userId = userId;
        req.shippingAddress = new OrderApiDto.ShippingAddress(recipient, phone, province, city, district, detail);
        req.items = new ArrayList<>();
        return req;
    }

    @Then("订单应创建成功")
    public void 订单应创建成功() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(201);
        assertThat(lastCreateResponse).isNotNull();
        assertThat(lastCreateResponse.getBody()).isNotNull();
    }

    @And("返回的订单包含 orderId")
    public void 返回的订单包含orderId() {
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().orderId).isNotNull();
    }

    @And("返回的订单 status 为 PENDING_PAYMENT")
    public void 返回的订单status为PENDING_PAYMENT() {
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().status).isEqualTo("PENDING_PAYMENT");
    }

    @Then("应创建失败")
    public void 应创建失败() {
        int status = lastResponseContext.getLastStatusCode();
        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    @And("应返回 400")
    public void 应返回400() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(400);
    }

    @And("应返回 404")
    public void 应返回404() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(404);
    }

    @And("应已发布 OrderCreated")
    public void 应已发布OrderCreated() {
        assertThat(orderEventCapture.wasOrderCreatedPublished()).isTrue();
    }

    @And("返回的订单总价为 {long} 分")
    public void 返回的订单总价为(long expectedTotalAmountCents) {
        assertThat(lastCreateResponse).isNotNull();
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().totalAmountCents).isEqualTo(expectedTotalAmountCents);
    }
}
