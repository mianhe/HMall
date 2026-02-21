package com.hmall.fulfillment.acceptance;

import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;

    public FulfillmentSmokeStepDefinitions(TestRestTemplate restTemplate, FulfillmentTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("请求 Fulfillment 健康检查接口")
    public void 请求健康检查接口() {
        ResponseEntity<Void> response = restTemplate.getForEntity("/api/fulfillment/health", Void.class);
        context.setLastStatusCode(response.getStatusCode().value());
    }

    @那么("应返回 {int}")
    public void 应返回状态码(int expectedStatus) {
        assertThat(context.getLastStatusCode()).isEqualTo(expectedStatus);
    }
}
