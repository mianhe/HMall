package com.hmall.smartinteraction.acceptance.config;

import com.hmall.smartinteraction.acceptance.SkillTestContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({ LlmWireMockConfig.class, McpWireMockConfig.class })
public class AiChatTestConfig {

    @Bean
    public SkillTestContext skillTestContext() {
        return new SkillTestContext();
    }
}

