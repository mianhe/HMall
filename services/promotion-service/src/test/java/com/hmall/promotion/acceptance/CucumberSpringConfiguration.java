package com.hmall.promotion.acceptance;

import com.hmall.promotion.PromotionServiceApplication;
import com.hmall.promotion.acceptance.config.PromotionAcceptanceTestConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(
        classes = {PromotionServiceApplication.class, PromotionAcceptanceTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
