package com.hmall.catalog.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 共享的 Then 步骤：应创建失败、应返回 404。
 * 各 Step Definition 在发起请求后需将响应状态码写入 {@link CatalogTestContext#setLastStatusCode(int)}。
 */
public class CommonAssertionStepDefinitions {

    private final CatalogTestContext context;

    public CommonAssertionStepDefinitions(CatalogTestContext context) {
        this.context = context;
    }

    @Then("应创建成功")
    public void 应创建成功() {
        assertThat(context.getLastStatusCode()).isEqualTo(201);
    }

    @Then("应创建失败")
    public void 应创建失败() {
        int status = context.getLastStatusCode();
        assertThat(status).isGreaterThanOrEqualTo(0);
        assertThat(status < 200 || status >= 300).as("期望状态码为 4xx/5xx 或 <200，实际 %d", status).isTrue();
    }

    @And("应返回 404")
    public void 应返回404() {
        assertThat(context.getLastStatusCode()).isEqualTo(404);
    }
}
