package com.hmall.promotion.acceptance;

import com.hmall.promotion.acceptance.config.PromotionTestContext;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PromotionSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final PromotionTestContext testContext;

    public PromotionSmokeStepDefinitions(TestRestTemplate restTemplate, PromotionTestContext testContext) {
        this.restTemplate = restTemplate;
        this.testContext = testContext;
    }

    @假如("Promotion 上下文已就绪")
    public void promotionContextReady() {
        ResponseEntity<Void> response = restTemplate.getForEntity("/api/promotion/health", Void.class);
        testContext.setLastStatusCode(response.getStatusCode().value());
    }

    @那么("验收测试应通过")
    public void acceptanceTestShouldPass() {
        assertTrue(testContext.getLastStatusCode() >= 200 && testContext.getLastStatusCode() < 300,
                "Expected 2xx but got " + testContext.getLastStatusCode());
    }
}
