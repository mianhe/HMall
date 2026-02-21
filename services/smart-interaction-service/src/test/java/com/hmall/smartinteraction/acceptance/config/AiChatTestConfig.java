package com.hmall.smartinteraction.acceptance.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({ LlmWireMockConfig.class, McpWireMockConfig.class })
public class AiChatTestConfig {
}
