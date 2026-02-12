package com.hmall.user.acceptance;

import com.hmall.catalog.acceptance.LastResponseContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 管理用户 feature 的 Step Definitions，按 user api.yaml 调用 API。
 */
public class UserStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;

    /** 用户名 -> 创建后得到的 id */
    private final Map<String, Long> usernameToId = new ConcurrentHashMap<>();

    private ResponseEntity<UserApiDto.Response> lastUserResponse;
    private ResponseEntity<List<UserApiDto.Response>> lastListResponse;

    public UserStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
    }

    private void setLastStatusCode(int statusCode) {
        lastResponseContext.setLastStatusCode(statusCode);
    }

    // ---------- 创建用户 ----------
    @When("用户创建用户 {string} 密码 {string}")
    public void 用户创建用户(String username, String password) {
        UserApiDto.Create body = new UserApiDto.Create();
        body.username = username;
        body.password = password;
        lastUserResponse = UserApiDto.postUser(restTemplate, body);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
        if (lastUserResponse.getStatusCode().is2xxSuccessful() && lastUserResponse.getBody() != null) {
            usernameToId.put(username, lastUserResponse.getBody().id);
        }
    }

    @And("返回的 userId 不为空")
    public void 返回的userId不为空() {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().id).isNotNull();
    }

    @And("返回的 username 为 {string}")
    public void 返回的username为(String expected) {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().username).isEqualTo(expected);
    }

    // ---------- 已存在用户（Given） ----------
    @Given("已存在用户 {string} 密码 {string}")
    public void 已存在用户(String username, String password) {
        UserApiDto.Create body = new UserApiDto.Create();
        body.username = username;
        body.password = password;
        ResponseEntity<UserApiDto.Response> res = UserApiDto.postUser(restTemplate, body);
        setLastStatusCode(res.getStatusCode().value());
        assertThat(res.getStatusCode().value()).isEqualTo(201);
        if (res.getBody() != null) {
            usernameToId.put(username, res.getBody().id);
        }
    }

    @Given("已存在用户 {string} 和 {string}")
    public void 已存在用户和(String username1, String username2) {
        已存在用户(username1, "pass1");
        已存在用户(username2, "pass2");
    }

    // ---------- 请求用户详情 ----------
    @When("用户请求用户 {string} 的详情")
    public void 用户请求用户详情(String username) {
        Long id = usernameToId.get(username);
        assertThat(id).as("用户「%s」应先存在", username).isNotNull();
        lastUserResponse = UserApiDto.getUserById(restTemplate, id);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @When("用户请求用户 ID {long} 的详情")
    public void 用户请求用户ID详情(long id) {
        lastUserResponse = UserApiDto.getUserById(restTemplate, id);
        setLastStatusCode(lastUserResponse.getStatusCode().value());
    }

    @And("返回的用户信息包含 username {string}")
    public void 返回的用户信息包含username(String expected) {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().username).isEqualTo(expected);
    }

    @And("返回的用户信息不含 passwordHash")
    public void 返回的用户信息不含passwordHash() {
        assertThat(lastUserResponse.getBody()).isNotNull();
        assertThat(lastUserResponse.getBody().passwordHash).isNull();
    }

    // ---------- 请求用户列表 ----------
    @When("用户请求用户列表")
    public void 用户请求用户列表() {
        lastListResponse = UserApiDto.getUsers(restTemplate);
        setLastStatusCode(lastListResponse.getStatusCode().value());
    }

    @And("返回的用户列表中至少有 {int} 个用户")
    public void 返回的用户列表中至少有(int minCount) {
        assertThat(lastListResponse).isNotNull();
        assertThat(lastListResponse.getBody()).hasSizeGreaterThanOrEqualTo(minCount);
    }

    @And("返回的列表中包含 username {string} 和 {string}")
    public void 返回的列表中包含username和(String name1, String name2) {
        List<String> usernames = lastListResponse.getBody().stream()
            .map(u -> u.username)
            .toList();
        assertThat(usernames).contains(name1, name2);
    }
}
