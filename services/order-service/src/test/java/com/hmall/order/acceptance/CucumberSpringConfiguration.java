package com.hmall.order.acceptance;

import com.hmall.order.OrderServiceApplication;
import com.hmall.order.acceptance.config.CatalogWireMockConfig;
import com.hmall.order.acceptance.config.OrderAcceptanceTestConfig;
import com.hmall.order.acceptance.config.UserWireMockConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@CucumberContextConfiguration
@SpringBootTest(
    classes = { OrderServiceApplication.class, OrderAcceptanceTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @Bean
    public TestRestTemplate testRestTemplate(@LocalServerPort int port) {
        TestRestTemplate t = new TestRestTemplate();
        t.getRestTemplate().setUriTemplateHandler(
            new org.springframework.web.util.DefaultUriBuilderFactory("http://localhost:" + port));
        return t;
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("catalog.base-url", () -> "http://localhost:" + CatalogWireMockConfig.getPort());
        registry.add("user.base-url", () -> "http://localhost:" + UserWireMockConfig.getPort());
    }
}
