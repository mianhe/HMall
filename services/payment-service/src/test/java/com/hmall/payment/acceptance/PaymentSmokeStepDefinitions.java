package com.hmall.payment.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class PaymentSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;

    public PaymentSmokeStepDefinitions(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @假如("Payment 上下文已就绪")
    public void paymentContextReady() {}

    @那么("验收测试应通过")
    public void assertionShouldPass() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/api/payments", Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(
                "冒烟失败：GET /api/payments 期望 2xx，实际 " + response.getStatusCode());
        }
    }
}
