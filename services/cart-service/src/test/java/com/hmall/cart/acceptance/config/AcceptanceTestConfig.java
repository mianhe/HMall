package com.hmall.cart.acceptance.config;

import com.hmall.cart.acceptance.CartSmokeStepDefinitions;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AcceptanceTestConfig {

    @Bean
    @Primary
    public CartSmokeStepDefinitions cartSmokeStepDefinitions(TestRestTemplate restTemplate) {
        return new CartSmokeStepDefinitions(restTemplate);
    }
}
