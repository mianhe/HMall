package com.hmall.smartinteraction.acceptance;

import com.hmall.smartinteraction.SmartInteractionApplication;
import com.hmall.smartinteraction.acceptance.config.AiChatTestConfig;
import com.hmall.smartinteraction.acceptance.config.LlmWireMockConfig;
import com.hmall.smartinteraction.acceptance.config.McpWireMockConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(
    classes = { SmartInteractionApplication.class, AiChatTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @DynamicPropertySource
    static void overrideAiProperties(DynamicPropertyRegistry registry) {
        registry.add("hmall.ai.default-provider", () -> "qwen");
        registry.add("hmall.ai.providers.qwen.api-key", () -> "test-key");
        registry.add("hmall.ai.providers.qwen.base-url",
            () -> "http://127.0.0.1:" + LlmWireMockConfig.getPort());
        registry.add("hmall.ai.providers.qwen.model", () -> "qwen-plus");
        registry.add("hmall.ai.providers.deepseek.api-key", () -> "test-key");
        registry.add("hmall.ai.providers.deepseek.base-url", () -> "http://127.0.0.1:19999");
        registry.add("hmall.ai.providers.deepseek.model", () -> "deepseek-chat");
        registry.add("hmall.ai.mcp.url",
            () -> "http://127.0.0.1:" + McpWireMockConfig.getPort() + "/mcp");
    }
}
