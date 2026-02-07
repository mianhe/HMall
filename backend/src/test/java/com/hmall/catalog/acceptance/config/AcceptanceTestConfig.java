package com.hmall.catalog.acceptance.config;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.catalog.acceptance.CategoryStepDefinitions;
import com.hmall.catalog.acceptance.DatabaseResetHook;
import com.hmall.catalog.acceptance.ProductStepDefinitions;
import com.hmall.catalog.infrastructure.persistence.CategoryJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SpuJpaRepository;

/**
 * 验收测试用 Bean 配置，与 Cucumber 解耦（避免被当作 glue 且带 @Component 报错）。
 */
@Configuration
public class AcceptanceTestConfig {

    @Bean
    @Primary
    public CategoryStepDefinitions categoryStepDefinitions(TestRestTemplate testRestTemplate) {
        return new CategoryStepDefinitions(testRestTemplate);
    }

    @Bean
    @Primary
    public ProductStepDefinitions productStepDefinitions(
            TestRestTemplate testRestTemplate,
            ObjectMapper objectMapper) {
        return new ProductStepDefinitions(testRestTemplate, objectMapper);
    }

    @Bean
    @Primary
    public DatabaseResetHook databaseResetHook(
            SpuJpaRepository spuJpaRepository,
            CategoryJpaRepository categoryJpaRepository) {
        return new DatabaseResetHook(spuJpaRepository, categoryJpaRepository);
    }
}
