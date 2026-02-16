package com.hmall.order.acceptance.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 测试用 WireMock 配置，模拟 User 服务。
 * user.base-url 指向 WireMock。
 */
@TestConfiguration
public class UserWireMockConfig {

    private static final WireMockServer wireMock = new WireMockServer(
        WireMockConfiguration.options().dynamicPort()
    );

    static {
        wireMock.start();
    }

    @Bean
    public WireMockServer userWireMock() {
        return wireMock;
    }

    public static int getPort() {
        return wireMock.port();
    }
}
