package com.hmall.order.acceptance;

import com.hmall.order.acceptance.config.LastOrderContext;
import com.hmall.order.acceptance.config.LastResponseContext;
import com.hmall.order.domain.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SupplementaryPurchaseStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final UserStubStepDefinitions userStub;
    private final CatalogStubStepDefinitions catalogStub;
    private final LastResponseContext lastResponseContext;
    private final LastOrderContext lastOrderContext;
    private final OrderRepository orderRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long deliveredOrderId;
    private List<Map<String, Object>> lastServicesList;
    private ResponseEntity<OrderApiDto.CreateResponse> lastCreateResponse;

    public SupplementaryPurchaseStepDefinitions(
            TestRestTemplate restTemplate,
            UserStubStepDefinitions userStub,
            CatalogStubStepDefinitions catalogStub,
            LastResponseContext lastResponseContext,
            LastOrderContext lastOrderContext,
            OrderRepository orderRepository) {
        this.restTemplate = restTemplate;
        this.userStub = userStub;
        this.catalogStub = catalogStub;
        this.lastResponseContext = lastResponseContext;
        this.lastOrderContext = lastOrderContext;
        this.orderRepository = orderRepository;
    }

    @Given("用户 {string} 已有一笔已交付订单包含 {string}")
    public void 用户已有一笔已交付订单包含(String username, String productName) {
        Long userId = userStub.getUserId(username);
        Long skuId = catalogStub.getSkuId(productName);
        assertThat(userId).isNotNull();
        assertThat(skuId).isNotNull();

        OrderLineItem item = new OrderLineItem(skuId, 1, 599900L, productName,
                OrderItemType.PHYSICAL, null, getSpuIdForProduct(productName));
        ShippingAddress addr = new ShippingAddress("测试", "13800000000", "北京", "北京", "海淀", "测试地址");
        Instant now = Instant.now();
        Order order = new Order(null, userId, OrderStatus.DELIVERED, 599900L,
                addr, List.of(item), true, false, now, now);
        Order saved = orderRepository.save(order);
        deliveredOrderId = saved.getOrderId();
    }

    @Given("用户 {string} 已有一笔待支付订单包含 {string}")
    public void 用户已有一笔待支付订单包含(String username, String productName) {
        Long userId = userStub.getUserId(username);
        Long skuId = catalogStub.getSkuId(productName);
        assertThat(userId).isNotNull();
        assertThat(skuId).isNotNull();

        OrderLineItem item = new OrderLineItem(skuId, 1, 599900L, productName,
                OrderItemType.PHYSICAL, null, getSpuIdForProduct(productName));
        ShippingAddress addr = new ShippingAddress("测试", "13800000000", "北京", "北京", "海淀", "测试地址");
        Instant now = Instant.now();
        Order order = new Order(null, userId, OrderStatus.PENDING_PAYMENT, 599900L,
                addr, List.of(item), false, false, now, now);
        Order saved = orderRepository.save(order);
        deliveredOrderId = saved.getOrderId();
    }

    @Given("用户 {string} 已为实体 skuId {long} 补购过服务 skuId {long}")
    public void 用户已补购过服务(String username, long relatedSkuId, long serviceSkuId) {
        Long userId = userStub.getUserId(username);
        assertThat(userId).isNotNull();

        OrderLineItem item = new OrderLineItem(serviceSkuId, 1, 29900L, "已购服务",
                OrderItemType.SERVICE, relatedSkuId, null);
        Instant now = Instant.now();
        Order order = new Order(null, userId, OrderStatus.PENDING_PAYMENT, 29900L,
                null, List.of(item), false, false, now, now);
        orderRepository.save(order);
    }

    @When("查询该订单的可补购服务列表")
    public void 查询该订单的可补购服务列表() {
        assertThat(deliveredOrderId).isNotNull();
        try {
            ResponseEntity<String> raw = restTemplate.exchange(
                "/api/orders/" + deliveredOrderId + "/purchasable-services",
                HttpMethod.GET, null, String.class
            );
            lastResponseContext.setLastStatusCode(raw.getStatusCode().value());
            if (raw.getStatusCode().is2xxSuccessful() && raw.getBody() != null) {
                lastServicesList = MAPPER.readValue(raw.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {});
            } else {
                lastServicesList = null;
            }
        } catch (RestClientResponseException e) {
            lastResponseContext.setLastStatusCode(e.getStatusCode().value());
            lastServicesList = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Then("应返回可补购服务列表")
    public void 应返回可补购服务列表() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
        assertThat(lastServicesList).isNotNull();
    }

    @And("列表包含服务 {string} 价格 {long} 分")
    public void 列表包含服务(String serviceName, long priceCents) {
        assertThat(lastServicesList).isNotNull();
        boolean found = lastServicesList.stream()
                .anyMatch(s -> serviceName.equals(s.get("serviceName"))
                        && ((Number) s.get("priceCents")).longValue() == priceCents);
        assertThat(found).as("应包含服务 %s 价格 %d", serviceName, priceCents).isTrue();
    }

    @And("列表为空")
    public void 列表为空() {
        assertThat(lastServicesList).isEmpty();
    }

    @When("用户 {string} 为该订单补购服务 {string} skuId {long} 关联实体 skuId {long}")
    public void 用户为该订单补购服务(String username, String serviceName, long serviceSkuId, long relatedSkuId) {
        doSupplementaryPurchase(username, serviceSkuId, relatedSkuId, true);
    }

    @When("用户 {string} 为该订单补购服务 {string} skuId {long} 关联实体 skuId {long} 不提供收货地址")
    public void 用户为该订单补购服务不提供收货地址(String username, String serviceName, long serviceSkuId, long relatedSkuId) {
        doSupplementaryPurchase(username, serviceSkuId, relatedSkuId, false);
    }

    @When("用户 {string} 补购服务 skuId {long} 关联实体 skuId {long} 不提供收货地址")
    public void 用户补购服务不提供收货地址(String username, long serviceSkuId, long relatedSkuId) {
        doSupplementaryPurchase(username, serviceSkuId, relatedSkuId, false);
    }

    @Then("补购订单应创建成功")
    public void 补购订单应创建成功() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(201);
        assertThat(lastCreateResponse).isNotNull();
        assertThat(lastCreateResponse.getBody()).isNotNull();
    }

    @And("补购订单 status 为 PENDING_PAYMENT")
    public void 补购订单status为PENDING_PAYMENT() {
        assertThat(lastCreateResponse.getBody()).isNotNull();
        assertThat(lastCreateResponse.getBody().status).isEqualTo("PENDING_PAYMENT");
    }

    private void doSupplementaryPurchase(String username, long serviceSkuId, long relatedSkuId, boolean withAddress) {
        Long userId = userStub.getUserId(username);
        assertThat(userId).isNotNull();

        OrderApiDto.CreateRequest req = new OrderApiDto.CreateRequest();
        req.userId = userId;
        req.items = List.of(new OrderApiDto.LineItemCreate(serviceSkuId, 1, relatedSkuId));
        if (withAddress) {
            req.shippingAddress = new OrderApiDto.ShippingAddress("测试", "13800000000", "北京", "北京", "海淀", "测试地址");
        }

        try {
            lastCreateResponse = restTemplate.exchange(
                "/api/orders", HttpMethod.POST,
                new HttpEntity<>(req, OrderApiDto.jsonHeaders()),
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

    private Long getSpuIdForProduct(String productName) {
        if (productName.contains("iPhone")) return 1L;
        if (productName.contains("耳机")) return 2L;
        return 1L;
    }
}
