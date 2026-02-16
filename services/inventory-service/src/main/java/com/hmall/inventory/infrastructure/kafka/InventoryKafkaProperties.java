package com.hmall.inventory.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Inventory 领域事件 Kafka topic 配置。
 */
@ConfigurationProperties(prefix = "inventory.kafka.topic")
public class InventoryKafkaProperties {

    private String stockReserved = "inventory.stock.reserved";
    private String stockReleased = "inventory.stock.released";

    public String getStockReserved() {
        return stockReserved;
    }

    public void setStockReserved(String stockReserved) {
        this.stockReserved = stockReserved;
    }

    public String getStockReleased() {
        return stockReleased;
    }

    public void setStockReleased(String stockReleased) {
        this.stockReleased = stockReleased;
    }
}
