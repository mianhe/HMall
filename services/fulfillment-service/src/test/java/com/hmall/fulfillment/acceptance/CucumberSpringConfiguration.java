package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.FulfillmentApplication;
import com.hmall.fulfillment.acceptance.config.AcceptanceTestConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(
    classes = { FulfillmentApplication.class, AcceptanceTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
