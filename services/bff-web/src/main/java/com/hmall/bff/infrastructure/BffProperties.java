package com.hmall.bff.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "bff")
public record BffProperties(
    Catalog catalog,
    User user,
    Order order,
    Inventory inventory,
    Payment payment,
    Activity activity,
    Cart cart,
    Proxy proxy
) {
    public record Catalog(String baseUrl) {}
    public record User(String baseUrl) {}
    public record Order(String baseUrl) {}
    public record Inventory(String baseUrl) {}
    public record Payment(String baseUrl) {}
    public record Activity(String baseUrl) {}
    public record Cart(String baseUrl) {}

    public record Proxy(
        List<String> allowedOrigins,
        int connectTimeoutMs,
        int readTimeoutMs
    ) {
        public Proxy {
            if (allowedOrigins == null) allowedOrigins = List.of(
                "http://127.0.0.1:5173", "http://127.0.0.1:5174",
                "http://localhost:5173", "http://localhost:5174"
            );
            if (connectTimeoutMs <= 0) connectTimeoutMs = 5000;
            if (readTimeoutMs <= 0) readTimeoutMs = 25000;
        }
    }
}
