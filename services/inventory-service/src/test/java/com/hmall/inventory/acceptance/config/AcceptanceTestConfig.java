package com.hmall.inventory.acceptance.config;

import com.hmall.inventory.acceptance.DatabaseResetHook;
import com.hmall.inventory.acceptance.EventCapture;
import com.hmall.inventory.acceptance.LastResponseContext;
import com.hmall.inventory.acceptance.OccupyStepDefinitions;
import com.hmall.inventory.acceptance.ReleaseStepDefinitions;
import com.hmall.inventory.acceptance.StockStepDefinitions;
import com.hmall.inventory.application.port.InventoryEventPublisher;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.infrastructure.persistence.ReservationJpaRepository;
import com.hmall.inventory.infrastructure.persistence.SkuStockJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.web.client.TestRestTemplate;

@Configuration
public class AcceptanceTestConfig {

    @Bean
    public LastResponseContext lastResponseContext() {
        return new LastResponseContext();
    }

    @Bean
    public EventCapture eventCapture() {
        return new EventCapture();
    }

    @Bean
    @Primary
    public InventoryEventPublisher inventoryEventPublisher(EventCapture eventCapture) {
        return eventCapture;
    }

    @Bean
    @Primary
    public OccupyStepDefinitions occupyStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            SkuStockRepository skuStockRepository,
            EventCapture eventCapture) {
        return new OccupyStepDefinitions(restTemplate, lastResponseContext, skuStockRepository, eventCapture);
    }

    @Bean
    @Primary
    public ReleaseStepDefinitions releaseStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext,
            SkuStockRepository skuStockRepository,
            EventCapture eventCapture) {
        return new ReleaseStepDefinitions(restTemplate, lastResponseContext, skuStockRepository, eventCapture);
    }

    @Bean
    @Primary
    public StockStepDefinitions stockStepDefinitions(
            TestRestTemplate restTemplate,
            LastResponseContext lastResponseContext) {
        return new StockStepDefinitions(restTemplate, lastResponseContext);
    }

    @Bean
    @Primary
    public DatabaseResetHook databaseResetHook(
            SkuStockJpaRepository skuStockJpaRepository,
            ReservationJpaRepository reservationJpaRepository,
            EventCapture eventCapture) {
        return new DatabaseResetHook(skuStockJpaRepository, reservationJpaRepository, eventCapture);
    }
}
