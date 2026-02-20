package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;

    public CartSmokeStepDefinitions(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @假如("Cart 上下文已就绪")
    public void cart_context_ready() {
        // 占位：上下文已启动
    }

    @那么("验收测试应通过")
    public void acceptance_test_should_pass() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/cart", String.class);
        int status = response.getStatusCode().value();
        assertTrue(status >= 200 && status < 300,
                "GET /api/cart 期望 2xx 实际 " + status);
    }
}
