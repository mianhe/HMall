package com.hmall.user.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class UserSmokeStepDefinitions {

    private final TestRestTemplate restTemplate;

    public UserSmokeStepDefinitions(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Given("User 上下文已就绪")
    public void userContextReady() {}

    @Then("验收测试应通过")
    public void assertionShouldPass() {
        ResponseEntity<Object> response = restTemplate.getForEntity("/api/users", Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new AssertionError(
                "冒烟失败：GET /api/users 期望 2xx，实际 " + response.getStatusCode());
        }
    }
}
