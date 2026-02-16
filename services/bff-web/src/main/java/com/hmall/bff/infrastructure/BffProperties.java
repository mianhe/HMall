package com.hmall.bff.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bff")
public record BffProperties(
    Catalog catalog,
    User user,
    Order order
) {
    public record Catalog(String baseUrl) {}
    public record User(String baseUrl) {}
    public record Order(String baseUrl) {}
}
