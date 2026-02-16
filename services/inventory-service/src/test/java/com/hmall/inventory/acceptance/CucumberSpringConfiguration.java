package com.hmall.inventory.acceptance;

import com.hmall.inventory.InventoryApplication;
import com.hmall.inventory.acceptance.config.AcceptanceTestConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(
    classes = { InventoryApplication.class, com.hmall.inventory.acceptance.config.AcceptanceTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {
}
