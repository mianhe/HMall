package com.hmall.user.acceptance;

import com.hmall.user.acceptance.config.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class CommonAssertionStepDefinitions {

    private final LastResponseContext context;

    public CommonAssertionStepDefinitions(LastResponseContext context) {
        this.context = context;
    }

    @Then("应创建成功")
    public void 应创建成功() {
        assertThat(context.getLastStatusCode()).isEqualTo(201);
    }

    @Then("应创建失败")
    public void 应创建失败() {
        assertNot2xx();
    }

    @And("应返回 404")
    public void 应返回404() {
        assertThat(context.getLastStatusCode()).isEqualTo(404);
    }

    @And("应返回 400")
    public void 应返回400() {
        assertThat(context.getLastStatusCode()).isEqualTo(400);
    }

    @And("应返回 401")
    public void 应返回401() {
        assertThat(context.getLastStatusCode()).isEqualTo(401);
    }

    @Then("应修改成功")
    public void 应修改成功() {
        assertThat(context.getLastStatusCode()).isEqualTo(200);
    }

    @Then("应删除成功")
    public void 应删除成功() {
        assertThat(context.getLastStatusCode()).isEqualTo(204);
    }

    @And("应返回 200")
    public void 应返回200() {
        assertThat(context.getLastStatusCode()).isEqualTo(200);
    }

    private void assertNot2xx() {
        int status = context.getLastStatusCode();
        assertThat(status).isGreaterThanOrEqualTo(0);
        assertThat(status < 200 || status >= 300).as("期望状态码为 4xx/5xx，实际 %d", status).isTrue();
    }
}
