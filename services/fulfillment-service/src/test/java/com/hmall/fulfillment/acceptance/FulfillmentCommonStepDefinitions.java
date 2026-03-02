package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentOrder;
import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentCommonStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final FulfillmentOrderRepository repository;

    public FulfillmentCommonStepDefinitions(TestRestTemplate restTemplate,
                                            FulfillmentTestContext context,
                                            FulfillmentOrderRepository repository) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.repository = repository;
    }

    @假如("已存在 CREATED 状态的履约单 orderId {long}")
    public void 已存在CREATED状态的履约单(long orderId) {
        createFulfillmentOrder(orderId);
    }

    @假如("已存在 ALLOCATING 状态的履约单 orderId {long}")
    public void 已存在ALLOCATING状态的履约单(long orderId) {
        createFulfillmentOrder(orderId);
    }

    @假如("已存在 SHIPPED 状态的履约单 orderId {long}")
    public void 已存在SHIPPED状态的履约单(long orderId) {
        createFulfillmentOrder(orderId);
        Long foId = context.getLastFulfillmentOrderId();
        shipFulfillmentOrder(foId);
    }

    @并且("该履约单状态应为 SHIPPED")
    public void 该履约单状态应为SHIPPED() {
        assertFulfillmentOrderStatus("SHIPPED");
    }

    @并且("该履约单状态应为 DELIVERED")
    public void 该履约单状态应为DELIVERED() {
        assertFulfillmentOrderStatus("DELIVERED");
    }

    @并且("该履约单状态应为 CANCELLED")
    public void 该履约单状态应为CANCELLED() {
        assertFulfillmentOrderStatus("CANCELLED");
    }

    @并且("该履约单状态应为 ALLOCATING")
    public void 该履约单状态应为ALLOCATING() {
        assertFulfillmentOrderStatus("ALLOCATING");
    }

    private void createFulfillmentOrder(long orderId) {
        Map<String, Object> body = Map.of(
            "orderId", orderId,
            "items", List.of(Map.of("skuId", 1001L, "quantity", 1)),
            "shippingAddress", Map.of(
                "recipientName", "测试用户", "phone", "13600000000",
                "province", "上海", "city", "上海", "district", "浦东新区", "detail", "测试地址"
            )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
            "/api/fulfillment/create",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<>() {}
        );
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) res.getBody().get("fulfillmentOrderIds");
        context.setLastFulfillmentOrderId(ids.get(0).longValue());
    }

    private void allocateFulfillmentOrder(Long fulfillmentOrderId) {
        restTemplate.exchange(
            "/api/fulfillment/" + fulfillmentOrderId + "/allocate",
            HttpMethod.POST,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private void shipFulfillmentOrder(Long fulfillmentOrderId) {
        Map<String, Object> body = Map.of("carrier", "顺丰", "trackingNumber", "SF0000000000");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
            "/api/fulfillment/" + fulfillmentOrderId + "/ship",
            HttpMethod.POST,
            new HttpEntity<>(body, headers),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    private void assertFulfillmentOrderStatus(String expectedStatus) {
        Long foId = context.getLastFulfillmentOrderId();
        assertThat(foId).isNotNull();
        FulfillmentOrder order = repository.findById(foId).orElse(null);
        assertThat(order).isNotNull();
        assertThat(order.getStatus().name()).isEqualTo(expectedStatus);
    }
}
