package com.hmall.promotion.acceptance.config;

import com.hmall.promotion.acceptance.CouponStepDefinitions;
import com.hmall.promotion.acceptance.CouponTemplateStepDefinitions;
import com.hmall.promotion.acceptance.PriceCalculationStepDefinitions;
import com.hmall.promotion.acceptance.PromotionSmokeStepDefinitions;
import com.hmall.promotion.application.UserSegmentResolver;
import com.hmall.promotion.infrastructure.persistence.CouponJpaRepository;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class PromotionAcceptanceTestConfig {

    @Bean
    public PromotionTestContext promotionTestContext() {
        return new PromotionTestContext();
    }

    @Bean
    @Primary
    public UserSegmentResolver userSegmentResolver() {
        return new TestUserSegmentResolver();
    }

    @Bean
    @Primary
    public PromotionSmokeStepDefinitions promotionSmokeStepDefinitions(
            TestRestTemplate restTemplate,
            PromotionTestContext testContext) {
        return new PromotionSmokeStepDefinitions(restTemplate, testContext);
    }

    @Bean
    @Primary
    public CouponTemplateStepDefinitions couponTemplateStepDefinitions(
            TestRestTemplate restTemplate,
            PromotionTestContext testContext) {
        return new CouponTemplateStepDefinitions(restTemplate, testContext);
    }

    @Bean
    @Primary
    public CouponStepDefinitions couponStepDefinitions(
            TestRestTemplate restTemplate,
            PromotionTestContext testContext,
            CouponJpaRepository couponJpaRepository) {
        return new CouponStepDefinitions(restTemplate, testContext, couponJpaRepository);
    }

    @Bean
    @Primary
    public PriceCalculationStepDefinitions priceCalculationStepDefinitions(
            TestRestTemplate restTemplate,
            PromotionTestContext testContext) {
        return new PriceCalculationStepDefinitions(restTemplate, testContext);
    }
}
