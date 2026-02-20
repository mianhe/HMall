package com.hmall.order.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class OrderSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;

    public OrderSmokeStepDefinitions(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @假如("Order 上下文已就绪")
    public void orderContextReady() {}

    @那么("验收测试应通过")
    public void assertionShouldPass() {
        ResponseEntity<Object> response = restTemplate.getForEntity(
            "/api/orders?userId=1&page=0&size=1", Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(
                "冒烟失败：GET /api/orders 期望 2xx，实际 " + response.getStatusCode());
        }
    }
}
