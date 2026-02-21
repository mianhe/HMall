package com.hmall.smartinteraction;

import com.hmall.smartinteraction.infrastructure.LlmProviderConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LlmProviderConfig.class)
public class SmartInteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartInteractionApplication.class, args);
    }
}
