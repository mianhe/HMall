package com.hmall.smartinteraction.acceptance.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class LlmWireMockConfig {

    private static final WireMockServer wireMock = new WireMockServer(
        WireMockConfiguration.options().dynamicPort()
    );

    static {
        wireMock.start();
    }

    @Bean
    public WireMockServer llmWireMock() {
        return wireMock;
    }

    public static int getPort() {
        return wireMock.port();
    }
}
