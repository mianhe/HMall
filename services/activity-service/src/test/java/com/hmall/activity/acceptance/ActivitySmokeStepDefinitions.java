package com.hmall.activity.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class ActivitySmokeStepDefinitions {

    private final TestRestTemplate restTemplate;

    public ActivitySmokeStepDefinitions(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @假如("Activity 上下文已就绪")
    public void activityContextReady() {
        // Spring context loaded; real check in 那么 step
    }

    @那么("验收测试应通过")
    public void assertionShouldPass() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/api/activities/recent", Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(
                "冒烟失败：GET /api/activities/recent 期望 2xx，实际 " + response.getStatusCode());
        }
    }
}
