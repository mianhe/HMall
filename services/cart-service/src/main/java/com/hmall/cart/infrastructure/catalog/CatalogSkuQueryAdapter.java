package com.hmall.cart.infrastructure.catalog;

import com.hmall.cart.application.port.SkuInfo;
import com.hmall.cart.application.port.SkuQueryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 通过 HTTP 调用 Catalog 服务的 SkuQueryPort 实现。
 * 契约见 docs/bounded-contexts/catalog/api.yaml：GET /api/skus/{id}。
 */
@Component
public class CatalogSkuQueryAdapter implements SkuQueryPort {

    private final RestTemplate restTemplate;
    private final String catalogBaseUrl;

    public CatalogSkuQueryAdapter(
            RestTemplate restTemplate,
            @Value("${catalog.base-url:http://localhost:8080}") String catalogBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogBaseUrl = catalogBaseUrl;
    }

    @Override
    public boolean exists(Long skuId) {
        return queryById(skuId).isPresent();
    }

    @Override
    public List<SkuInfo> queryByIds(List<Long> skuIds) {
        return skuIds.stream()
            .map(this::queryById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    @Override
    public Optional<SkuInfo> queryById(Long skuId) {
        String url = catalogBaseUrl + "/api/skus/" + skuId;
        try {
            CatalogSkuResponse res = restTemplate.getForObject(url, CatalogSkuResponse.class);
            if (res == null) return Optional.empty();
            String name = buildDisplayName(res.spuName(), res.displayName(), buildSpecDisplayName(res.specValues()));
            BigDecimal price = res.priceCents() != null
                ? BigDecimal.valueOf(res.priceCents()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            return Optional.of(new SkuInfo(
                res.id(),
                res.spuId(),
                res.productType() != null ? res.productType() : "PHYSICAL",
                name,
                price,
                null,
                true
            ));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public List<AvailableService> queryAvailableServices(Long targetSpuId) {
        String url = catalogBaseUrl + "/api/products/" + targetSpuId + "/available-services";
        try {
            ResponseEntity<List<AvailableServiceResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            List<AvailableServiceResponse> responses = response.getBody();
            if (responses == null) {
                return List.of();
            }
            return responses.stream()
                .map(this::toAvailableService)
                .toList();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return List.of();
            }
            throw e;
        }
    }

    private static String buildDisplayName(String spuName, String skuDisplayName, String specDisplayName) {
        String p = spuName != null ? spuName.trim() : "";
        String s = skuDisplayName != null ? skuDisplayName.trim() : "";
        String spec = specDisplayName != null ? specDisplayName.trim() : "";
        if (s.isEmpty() && !spec.isEmpty()) {
            s = spec;
        }
        if (p.isEmpty() && s.isEmpty()) return "商品";
        if (p.isEmpty()) return s;
        if (s.isEmpty() || s.equals(p)) return p;
        return p + " " + s;
    }

    private static String buildSpecDisplayName(List<SpecValueResponse> specValues) {
        if (specValues == null || specValues.isEmpty()) {
            return "";
        }
        return specValues.stream()
            .map(v -> v.optionValue() != null ? v.optionValue().trim() : "")
            .filter(v -> !v.isEmpty())
            .distinct()
            .reduce((a, b) -> a + " " + b)
            .orElse("");
    }

    private AvailableService toAvailableService(AvailableServiceResponse response) {
        List<AvailableServiceSku> bindings = response.bindings() == null
            ? List.of()
            : response.bindings().stream()
                .map(b -> new AvailableServiceSku(
                    b.bindingId(),
                    b.serviceSkuId(),
                    BigDecimal.valueOf(b.priceCents()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                ))
                .toList();
        return new AvailableService(response.serviceSpuId(), response.name(), bindings);
    }

    private record CatalogSkuResponse(
        Long id,
        Long spuId,
        Long priceCents,
        String displayName,
        String spuName,
        String productType,
        List<SpecValueResponse> specValues
    ) {}

    private record SpecValueResponse(String dimensionName, String optionValue) {}

    private record AvailableServiceResponse(Long serviceSpuId, String name, List<AvailableServiceSkuResponse> bindings) {}
    private record AvailableServiceSkuResponse(Long bindingId, Long serviceSkuId, Long priceCents) {}
}
