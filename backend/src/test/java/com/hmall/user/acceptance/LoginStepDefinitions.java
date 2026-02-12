package com.hmall.user.acceptance;

import com.hmall.catalog.acceptance.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户登录 feature 的 Step Definitions，按 user api.yaml 调用 API。
 */
public class LoginStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;

    private ResponseEntity<LoginApiDto.Response> lastLoginResponse;

    public LoginStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
    }

    private void setLastStatusCode(int statusCode) {
        lastResponseContext.setLastStatusCode(statusCode);
    }

    @When("用户使用 {string} 和 {string} 登录")
    public void 用户使用登录(String username, String password) {
        LoginApiDto.Request body = new LoginApiDto.Request();
        body.username = username;
        body.password = password;
        lastLoginResponse = LoginApiDto.postLogin(restTemplate, body);
        setLastStatusCode(lastLoginResponse.getStatusCode().value());
    }

    @Then("应登录成功")
    public void 应登录成功() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
    }

    @Then("应登录失败")
    public void 应登录失败() {
        int status = lastResponseContext.getLastStatusCode();
        assertThat(status).isGreaterThanOrEqualTo(400);
    }

    @And("返回的 token 不为空")
    public void 返回的token不为空() {
        assertThat(lastLoginResponse.getBody()).isNotNull();
        assertThat(lastLoginResponse.getBody().token).isNotBlank();
    }
}
