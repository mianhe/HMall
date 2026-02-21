package com.hmall.fulfillment.acceptance;

import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentQueryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;

    public FulfillmentQueryStepDefinitions(TestRestTemplate restTemplate,
                                           FulfillmentTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("按 fulfillmentOrderId 查询该履约单")
    public void 按fulfillmentOrderId查询该履约单() {
        Long foId = context.getLastFulfillmentOrderId();
        getById(foId);
    }

    @当("按 fulfillmentOrderId {long} 查询履约单")
    public void 按fulfillmentOrderId查询履约单(long foId) {
        getById(foId);
    }

    @当("按 orderId {long} 查询履约单")
    public void 按orderId查询履约单(long orderId) {
        try {
            ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
                "/api/fulfillment?orderId=" + orderId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            context.setLastStatusCode(res.getStatusCode().value());
            context.setLastResponseBody(Map.of("list", res.getBody()));
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
            context.setLastResponseBody(null);
        }
    }

    @并且("返回结果包含履约单详情（含商品明细、状态、物流信息）")
    public void 返回结果包含履约单详情() {
        Map<String, Object> body = context.getLastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body.get("fulfillmentOrderId")).isNotNull();
        assertThat(body.get("orderId")).isNotNull();
        assertThat(body.get("status")).isNotNull();
        assertThat(body.get("items")).isNotNull();
        assertThat(body.get("shippingAddress")).isNotNull();
    }

    @并且("返回结果包含该 orderId 的所有履约单")
    public void 返回结果包含该orderId的所有履约单() {
        Map<String, Object> body = context.getLastResponseBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) body.get("list");
        assertThat(list).isNotEmpty();
    }

    private void getById(Long foId) {
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "/api/fulfillment/" + foId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            context.setLastStatusCode(res.getStatusCode().value());
            context.setLastResponseBody(res.getBody());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
            context.setLastResponseBody(null);
        }
    }
}
