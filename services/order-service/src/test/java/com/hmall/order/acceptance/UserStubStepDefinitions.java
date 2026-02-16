package com.hmall.order.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.cucumber.java.en.Given;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试用 User 桩：WireMock 模拟 User 服务，存储 username -> userId。
 */
public class UserStubStepDefinitions {

    private final WireMockServer wireMock;
    private final Map<String, Long> usernameToUserId = new ConcurrentHashMap<>();
    private long nextUserId = 1;

    public UserStubStepDefinitions(WireMockServer userWireMock) {
        this.wireMock = userWireMock;
    }

    @Given("已存在用户 {string} 密码 {string}")
    public void 已存在用户(String username, String password) {
        long userId = nextUserId++;
        usernameToUserId.put(username, userId);
        String json = """
            {"id":%d,"username":"%s","createdAt":"2024-01-01T00:00:00Z"}
            """.formatted(userId, username.replace("\"", "\\\""));
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/users/" + userId))
                .willReturn(WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(json))
        );
    }

    @Given("用户 {long} 不存在")
    public void 用户不存在(long userId) {
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathEqualTo("/api/users/" + userId))
                .willReturn(WireMock.aResponse().withStatus(404))
        );
    }

    public Long getUserId(String username) {
        return usernameToUserId.get(username);
    }
}
