package com.hmall.user.acceptance.config;

import com.hmall.user.acceptance.*;
import com.hmall.user.infrastructure.persistence.AddressJpaRepository;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class UserAcceptanceTestConfig {

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    @Primary
    public UserSmokeStepDefinitions userSmokeStepDefinitions(TestRestTemplate testRestTemplate) {
        return new UserSmokeStepDefinitions(testRestTemplate);
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

    @Bean
    @Primary
    public CommonAssertionStepDefinitions commonAssertionStepDefinitions(LastResponseContext lastResponseContext) {
        return new CommonAssertionStepDefinitions(lastResponseContext);
    }

    @Bean
    @Primary
    public AddressStepDefinitions addressStepDefinitions(
            TestRestTemplate testRestTemplate,
            LastResponseContext lastResponseContext,
            UserStepDefinitions userStepDefinitions) {
        return new AddressStepDefinitions(testRestTemplate, lastResponseContext, userStepDefinitions);
    }

    @Bean
    @Primary
    public DatabaseResetHook databaseResetHook(UserJpaRepository userJpaRepository, AddressJpaRepository addressJpaRepository) {
        return new DatabaseResetHook(userJpaRepository, addressJpaRepository);
    }
}

