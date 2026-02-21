package com.hmall.fulfillment.acceptance;

import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentCancelStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;

    public FulfillmentCancelStepDefinitions(TestRestTemplate restTemplate,
                                            FulfillmentTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("Order 调用取消履约单接口 orderId {long}")
    public void order调用取消履约单接口(long orderId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("orderId", orderId);
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                "/api/fulfillment/cancel",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<>() {}
            );
            context.setLastStatusCode(res.getStatusCode().value());
            context.setLastResponseBody(res.getBody());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
            context.setLastResponseBody(null);
        }
    }

    @并且("返回的 cancelledCount 应为 {int}")
    public void 返回的cancelledCount应为(int expected) {
        assertThat(context.getLastResponseBody()).isNotNull();
        assertThat(((Number) context.getLastResponseBody().get("cancelledCount")).intValue()).isEqualTo(expected);
    }
}
