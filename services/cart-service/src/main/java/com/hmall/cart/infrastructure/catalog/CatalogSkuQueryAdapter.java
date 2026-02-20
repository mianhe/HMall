package com.hmall.cart.infrastructure.catalog;

import com.hmall.cart.application.port.SkuInfo;
import com.hmall.cart.application.port.SkuQueryPort;
import org.springframework.beans.factory.annotation.Value;
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
            String name = buildDisplayName(res.spuName(), res.displayName());
            BigDecimal price = res.priceCents() != null
                ? BigDecimal.valueOf(res.priceCents()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            return Optional.of(new SkuInfo(res.id(), name, price, null, true));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private static String buildDisplayName(String spuName, String skuDisplayName) {
        String p = spuName != null ? spuName.trim() : "";
        String s = skuDisplayName != null ? skuDisplayName.trim() : "";
        if (p.isEmpty() && s.isEmpty()) return "商品";
        if (p.isEmpty()) return s;
        if (s.isEmpty() || s.equals(p)) return p;
        return p + " " + s;
    }

    private record CatalogSkuResponse(Long id, Long spuId, Long priceCents, String displayName, String spuName) {}
}
