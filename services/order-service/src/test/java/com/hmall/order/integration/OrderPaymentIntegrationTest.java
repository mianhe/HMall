package com.hmall.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hmall.order.OrderServiceApplication;
import com.hmall.order.acceptance.OrderApiDto;
import com.hmall.order.domain.Order;
import com.hmall.order.domain.OrderRepository;
import com.hmall.order.domain.OrderStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order → Payment 集成测试：使用 WireMock 模拟 Payment（及 Catalog、User、Inventory），
 * 验证下单时调用 POST /api/payments、取消已支付订单时调用 POST /api/payments/refund。
 */
@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OrderPaymentIntegrationTest {

    private static final WireMockServer wireMock = new WireMockServer(
            WireMockConfiguration.options().dynamicPort()
    );

    private static final long USER_ID = 1L;
    private static final long SKU_ID = 123L;
    private static final int QUANTITY = 2;

    @MockBean
    private com.hmall.order.application.port.OrderOutboundEventPublisher orderOutboundEventPublisher;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + wireMock.port();
        registry.add("catalog.base-url", () -> base);
        registry.add("user.base-url", () -> base);
        registry.add("inventory.base-url", () -> base);
        registry.add("payment.base-url", () -> base);
    }

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        wireMock.resetAll();
        wireMock.stubFor(get(urlPathMatching("/api/users/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        wireMock.stubFor(get(urlPathMatching("/api/skus/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":123,\"spuId\":1,\"priceCents\":599900,\"displayName\":\"iPhone 15\"}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/inventory/occupy"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/inventory/release"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentId\":1001,\"orderId\":1,\"amountCents\":1199800,\"status\":\"PENDING\",\"payUrl\":\"https://pay.example/1001\"}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/payments/refund"))
                .willReturn(aResponse().withStatus(200)));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void placeOrder_should_call_payment_create() throws Exception {
        OrderApiDto.CreateRequest req = new OrderApiDto.CreateRequest();
        req.userId = USER_ID;
        req.shippingAddress = new OrderApiDto.ShippingAddress("张三", "13800138000", "上海", "上海", "浦东新区", "测试地址");
        req.items = List.of(new OrderApiDto.LineItemCreate(SKU_ID, QUANTITY));

        ResponseEntity<OrderApiDto.CreateResponse> createResp = restTemplate.exchange(
                "/api/orders",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(req, OrderApiDto.jsonHeaders()),
                OrderApiDto.CreateResponse.class);

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody()).isNotNull();
        Long orderId = createResp.getBody().orderId;
        long expectedAmountCents = 599900L * QUANTITY; // 1199800

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/payments")));
        List<LoggedRequest> createRequests = wireMock.findAll(postRequestedFor(urlPathEqualTo("/api/payments")));
        LoggedRequest ourRequest = createRequests.stream()
                .filter(r -> {
                    try {
                        JsonNode n = new ObjectMapper().readTree(r.getBodyAsString());
                        return n.has("orderId") && n.get("orderId").asLong() == orderId;
                    } catch (Exception e) { return false; }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No POST /api/payments with orderId " + orderId));
        JsonNode node = new ObjectMapper().readTree(ourRequest.getBodyAsString());
        assertThat(node.get("amountCents").asLong()).isEqualTo(expectedAmountCents);
    }

    @Test
    void cancel_paid_order_should_call_payment_refund() throws Exception {
        OrderApiDto.CreateRequest req = new OrderApiDto.CreateRequest();
        req.userId = USER_ID;
        req.shippingAddress = new OrderApiDto.ShippingAddress("李四", "13900139000", "北京", "北京", "朝阳区", "测试地址");
        req.items = List.of(new OrderApiDto.LineItemCreate(SKU_ID, 1));

        ResponseEntity<OrderApiDto.CreateResponse> createResp = restTemplate.exchange(
                "/api/orders",
                org.springframework.http.HttpMethod.POST,
                new HttpEntity<>(req, OrderApiDto.jsonHeaders()),
                OrderApiDto.CreateResponse.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long orderId = createResp.getBody().orderId;

        Order order = orderRepository.findById(orderId).orElseThrow();
        Order paidOrder = new Order(
                order.getOrderId(), order.getUserId(), OrderStatus.PAID,
                order.getTotalAmountCents(), order.getShippingAddress(), order.getItems(),
                order.getCreatedAt(), Instant.now());
        orderRepository.save(paidOrder);

        ResponseEntity<Void> cancelResp = restTemplate.postForEntity(
                "/api/orders/" + orderId + "/cancel", null, Void.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        wireMock.verify(postRequestedFor(urlPathEqualTo("/api/payments/refund")));
        List<LoggedRequest> refundRequests = wireMock.findAll(postRequestedFor(urlPathEqualTo("/api/payments/refund")));
        LoggedRequest ourRefund = refundRequests.stream()
                .filter(r -> {
                    try {
                        JsonNode n = new ObjectMapper().readTree(r.getBodyAsString());
                        return n.has("orderId") && n.get("orderId").asLong() == orderId;
                    } catch (Exception e) { return false; }
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("No POST /api/payments/refund with orderId " + orderId));
        JsonNode refundNode = new ObjectMapper().readTree(ourRefund.getBodyAsString());
        assertThat(refundNode.get("orderId").asLong()).isEqualTo(orderId);
    }
}
