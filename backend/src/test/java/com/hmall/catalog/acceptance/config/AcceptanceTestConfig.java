package com.hmall.catalog.acceptance.config;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.catalog.acceptance.CatalogTestContext;
import com.hmall.catalog.acceptance.LastResponseContext;
import com.hmall.catalog.acceptance.CategoryStepDefinitions;
import com.hmall.catalog.acceptance.CommonAssertionStepDefinitions;
import com.hmall.catalog.acceptance.DatabaseResetHook;
import com.hmall.catalog.acceptance.ProductStepDefinitions;
import com.hmall.catalog.acceptance.SpecDimensionStepDefinitions;
import com.hmall.catalog.acceptance.SkuStepDefinitions;
import com.hmall.catalog.infrastructure.persistence.CategoryJpaRepository;
import com.hmall.catalog.infrastructure.persistence.SpuJpaRepository;
import com.hmall.user.infrastructure.persistence.UserJpaRepository;

/**
 * 验收测试用 Bean 配置，与 Cucumber 解耦（避免被当作 glue 且带 @Component 报错）。
 */
@Configuration
public class AcceptanceTestConfig {

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    public CatalogTestContext catalogTestContext(LastResponseContext lastResponseContext) {
        return new CatalogTestContext(lastResponseContext);
    }

    @Bean
    @Primary
    public CategoryStepDefinitions categoryStepDefinitions(
            TestRestTemplate testRestTemplate,
            CatalogTestContext catalogTestContext) {
        return new CategoryStepDefinitions(testRestTemplate, catalogTestContext);
    }

    @Bean
    @Primary
    public ProductStepDefinitions productStepDefinitions(
            TestRestTemplate testRestTemplate,
            ObjectMapper objectMapper,
            CatalogTestContext catalogTestContext) {
        return new ProductStepDefinitions(testRestTemplate, objectMapper, catalogTestContext);
    }

    @Bean
    @Primary
    public SpecDimensionStepDefinitions specDimensionStepDefinitions(
            TestRestTemplate testRestTemplate,
            CatalogTestContext catalogTestContext,
            ObjectMapper objectMapper) {
        return new SpecDimensionStepDefinitions(testRestTemplate, catalogTestContext, objectMapper);
    }

    @Bean
    @Primary
    public SkuStepDefinitions skuStepDefinitions(
            TestRestTemplate testRestTemplate,
            CatalogTestContext catalogTestContext,
            ObjectMapper objectMapper) {
        return new SkuStepDefinitions(testRestTemplate, catalogTestContext, objectMapper);
    }

    @Bean
    @Primary
    public CommonAssertionStepDefinitions commonAssertionStepDefinitions(LastResponseContext lastResponseContext) {
        return new CommonAssertionStepDefinitions(lastResponseContext);
    }

    @Bean
    @Primary
    public DatabaseResetHook databaseResetHook(
            SpuJpaRepository spuJpaRepository,
            CategoryJpaRepository categoryJpaRepository,
            UserJpaRepository userJpaRepository) {
        return new DatabaseResetHook(spuJpaRepository, categoryJpaRepository, userJpaRepository);
    }
}
