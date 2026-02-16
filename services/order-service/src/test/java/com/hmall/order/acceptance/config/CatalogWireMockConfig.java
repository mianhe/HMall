package com.hmall.order.acceptance.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * 测试用 WireMock 配置，模拟 Catalog 服务。
 * catalog.base-url 指向 WireMock。
 */
@TestConfiguration
public class CatalogWireMockConfig {

    private static final WireMockServer wireMock = new WireMockServer(
        WireMockConfiguration.options().dynamicPort()
    );

    static {
        wireMock.start();
    }

    @Bean
    public WireMockServer catalogWireMock() {
        return wireMock;
    }

    /** 供 Cucumber 测试设置 catalog.base-url */
    public static int getPort() {
        return wireMock.port();
    }
}
