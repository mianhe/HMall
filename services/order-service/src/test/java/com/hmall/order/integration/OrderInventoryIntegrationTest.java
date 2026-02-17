package com.hmall.order.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.hmall.order.OrderServiceApplication;
import com.hmall.order.acceptance.OrderApiDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.hmall.order.application.port.OrderOutboundEventPublisher;
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

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order → Inventory 集成测试：使用 WireMock 模拟 Inventory（及 Catalog、User），
 * 验证下单时调用 POST /api/inventory/occupy、取消时调用 POST /api/inventory/release。
 */
@SpringBootTest(
    classes = OrderServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class OrderInventoryIntegrationTest {

    private static final WireMockServer wireMock = new WireMockServer(
            WireMockConfiguration.options().dynamicPort()
    );

    private static final long USER_ID = 1L;
    private static final long SKU_ID = 123L;
    private static final int QUANTITY = 2;

    @MockBean
    private OrderOutboundEventPublisher orderOutboundEventPublisher;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        String base = "http://localhost:" + wireMock.port();
        registry.add("catalog.base-url", () -> base);
        registry.add("user.base-url", () -> base);
        registry.add("inventory.base-url", () -> base);
    }

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
        wireMock.resetAll();
        // User: GET /api/users/{id}（UserExistsAdapter 需要 JSON 才能解析）
        wireMock.stubFor(get(urlPathMatching("/api/users/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
        // Catalog: GET /api/skus/{id}
        wireMock.stubFor(get(urlPathMatching("/api/skus/[0-9]+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":123,\"spuId\":1,\"priceCents\":599900,\"displayName\":\"iPhone 15\"}")));
        // Inventory: occupy & release
        wireMock.stubFor(post(urlPathEqualTo("/api/inventory/occupy"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true}")));
        wireMock.stubFor(post(urlPathEqualTo("/api/inventory/release"))
                .willReturn(aResponse().withStatus(200).withBody("{\"success\":true}")));
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @Test
    void placeOrder_and_cancel_should_call_inventory_occupy_then_release() throws Exception {
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

        // 验证 Order 调用了 Inventory 占用
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/api/inventory/occupy")));
        List<LoggedRequest> occupyRequests = wireMock.findAll(postRequestedFor(urlPathEqualTo("/api/inventory/occupy")));
        assertThat(occupyRequests).hasSize(1);
        String body = occupyRequests.get(0).getBodyAsString();
        JsonNode node = new ObjectMapper().readTree(body);
        assertThat(node.get("orderId").asLong()).isEqualTo(orderId);
        assertThat(node.get("items")).isNotNull();
        assertThat(node.get("items").size()).isEqualTo(1);
        assertThat(node.get("items").get(0).get("skuId").asLong()).isEqualTo(SKU_ID);
        assertThat(node.get("items").get(0).get("quantity").asInt()).isEqualTo(QUANTITY);

        // 取消订单，验证释放
        ResponseEntity<Void> cancelResp = restTemplate.postForEntity(
                "/api/orders/" + orderId + "/cancel", null, Void.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/api/inventory/release")));
        List<LoggedRequest> releaseRequests = wireMock.findAll(postRequestedFor(urlPathEqualTo("/api/inventory/release")));
        assertThat(releaseRequests).hasSize(1);
        String releaseBody = releaseRequests.get(0).getBodyAsString();
        JsonNode releaseNode = new ObjectMapper().readTree(releaseBody);
        assertThat(releaseNode.get("orderId").asLong()).isEqualTo(orderId);
    }
}
