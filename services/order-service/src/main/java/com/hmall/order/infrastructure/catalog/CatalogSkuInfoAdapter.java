package com.hmall.order.infrastructure.catalog;

import com.hmall.order.application.port.SkuInfoPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 通过 HTTP 调用 Catalog 服务的 SkuInfoPort 实现。
 */
@Component
public class CatalogSkuInfoAdapter implements SkuInfoPort {

    private final RestTemplate restTemplate;
    private final String catalogBaseUrl;

    public CatalogSkuInfoAdapter(
            RestTemplate restTemplate,
            @Value("${catalog.base-url:http://localhost:8080}") String catalogBaseUrl) {
        this.restTemplate = restTemplate;
        this.catalogBaseUrl = catalogBaseUrl;
    }

    @Override
    public SkuInfo getById(Long skuId) {
        String url = catalogBaseUrl + "/api/skus/" + skuId;
        try {
            CatalogSkuResponse response = restTemplate.getForObject(url, CatalogSkuResponse.class);
            if (response == null) {
                throw new IllegalArgumentException("SKU 不存在");
            }
            long price = response.priceCents() != null ? response.priceCents() : 0L;
            String displayName = buildDisplayName(
                response.spuName(),
                response.displayName(),
                buildSpecDisplayName(response.specValues())
            );
            String productType = response.productType() != null ? response.productType() : "PHYSICAL";
            return new SkuInfo(response.id(), response.spuId(), price, displayName, productType);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("SKU 不存在");
            }
            throw e;
        }
    }

    @Override
    public Long findBoundServicePriceCents(Long targetSpuId, Long serviceSkuId) {
        String url = catalogBaseUrl + "/api/products/" + targetSpuId + "/available-services";
        try {
            ResponseEntity<List<AvailableServiceResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
            List<AvailableServiceResponse> body = response.getBody();
            if (body == null) {
                return null;
            }
            return body.stream()
                .filter(service -> service.bindings() != null)
                .flatMap(service -> service.bindings().stream())
                .filter(binding -> serviceSkuId.equals(binding.serviceSkuId()))
                .map(AvailableServiceSkuResponse::priceCents)
                .findFirst()
                .orElse(null);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return null;
            }
            throw e;
        }
    }

    /** 拼接「商品名 + SKU 规格」，便于订单列表展示 */
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

    /** Catalog GET /api/skus/{id} 响应结构 */
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
