package com.hmall.smartinteraction.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "hmall.ai")
public record LlmProviderConfig(
    String defaultProvider,
    Map<String, Provider> providers,
    Mcp mcp
) {
    public record Provider(String apiKey, String baseUrl, String model) {}

    public record Mcp(String url) {}

    public Provider resolveProvider(String providerId) {
        String id = (providerId != null && !providerId.isBlank()) ? providerId : defaultProvider;
        Provider provider = providers.get(id);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown AI provider: " + id);
        }
        return provider;
    }
}
