package com.hmall.user.acceptance.config;

import com.hmall.user.acceptance.UserSmokeStepDefinitions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * User 上下文验收测试 Bean 配置。
 */
@Configuration
public class UserAcceptanceTestConfig {

    @Bean
    @Primary
    public UserSmokeStepDefinitions userSmokeStepDefinitions() {
        return new UserSmokeStepDefinitions();
    }
}
