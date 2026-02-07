package com.hmall.catalog.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.hmall.catalog.acceptance.config.AcceptanceTestConfig;
import com.hmall.HmallApplication;

/**
 * Cucumber 与 Spring Boot 集成：启动应用。Bean 由 AcceptanceTestConfig 提供。
 */
@CucumberContextConfiguration
@SpringBootTest(
    classes = { HmallApplication.class, AcceptanceTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
