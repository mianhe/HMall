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
            String displayName = buildDisplayName(response.spuName(), response.displayName());
            return new SkuInfo(response.id(), price, displayName);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalArgumentException("SKU 不存在");
            }
            throw e;
        }
    }

    /** 拼接「商品名 + SKU 规格」，便于订单列表展示 */
    private static String buildDisplayName(String spuName, String skuDisplayName) {
        String p = spuName != null ? spuName.trim() : "";
        String s = skuDisplayName != null ? skuDisplayName.trim() : "";
        if (p.isEmpty() && s.isEmpty()) return "商品";
        if (p.isEmpty()) return s;
        if (s.isEmpty() || s.equals(p)) return p;
        return p + " " + s;
    }

    /** Catalog GET /api/skus/{id} 响应结构 */
    private record CatalogSkuResponse(Long id, Long spuId, Long priceCents, String displayName, String spuName) {}
}
