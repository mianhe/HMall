package com.hmall.user.acceptance.config;

import com.hmall.catalog.acceptance.LastResponseContext;
import com.hmall.user.acceptance.LoginStepDefinitions;
import com.hmall.user.acceptance.UserSmokeStepDefinitions;
import com.hmall.user.acceptance.UserStepDefinitions;
import org.springframework.boot.test.web.client.TestRestTemplate;
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

    @Bean
    @Primary
    public UserStepDefinitions userStepDefinitions(
            TestRestTemplate testRestTemplate,
            LastResponseContext lastResponseContext) {
        return new UserStepDefinitions(testRestTemplate, lastResponseContext);
    }

    @Bean
    @Primary
    public LoginStepDefinitions loginStepDefinitions(
            TestRestTemplate testRestTemplate,
            LastResponseContext lastResponseContext) {
        return new LoginStepDefinitions(testRestTemplate, lastResponseContext);
    }
}
