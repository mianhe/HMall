package com.hmall.cart.acceptance.config;

import com.hmall.cart.acceptance.CartTestContext;
import com.hmall.cart.acceptance.StubSkuQueryPort;
import com.hmall.cart.application.port.SkuQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AcceptanceTestConfig {

    @Bean
    public CartTestContext cartTestContext() {
        return new CartTestContext();
    }

    @Bean
    @Primary
    public SkuQueryPort stubSkuQueryPort() {
        return new StubSkuQueryPort();
    }
}
