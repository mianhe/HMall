package com.hmall.order.infrastructure.catalog;

import com.hmall.order.application.port.SkuInfoPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

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
            return new SkuInfo(
                response.id(),
                price,
                response.displayName() != null ? response.displayName() : ""
            );
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("SKU 不存在");
            }
            throw e;
        }
    }

    /** Catalog GET /api/skus/{id} 响应结构 */
    private record CatalogSkuResponse(Long id, Long spuId, Long priceCents, String displayName) {}
}
