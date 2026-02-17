package com.hmall.order.infrastructure.inventory;

import com.hmall.order.application.port.ReleaseInventoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 HTTP 调用 Inventory 服务的 ReleaseInventoryPort 实现。
 * 契约见 docs/bounded-contexts/inventory/api.yaml：POST /api/inventory/release。
 */
@Component
@ConditionalOnProperty(name = "inventory.base-url")
public class RestReleaseInventoryAdapter implements ReleaseInventoryPort {

    private final RestTemplate restTemplate;
    private final String releaseUrl;

    public RestReleaseInventoryAdapter(
            RestTemplate restTemplate,
            @Value("${inventory.base-url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplate;
        this.releaseUrl = inventoryBaseUrl + "/api/inventory/release";
    }

    @Override
    public void release(Long orderId) {
        ReleaseRequest request = new ReleaseRequest(orderId);
        try {
            restTemplate.exchange(
                    releaseUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class);
        } catch (RestClientResponseException e) {
            throw new InventoryCallException("释放库存失败: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new InventoryUnavailableException("库存服务暂时不可用，请稍后重试", e);
        }
    }

    /** 与 api.yaml ReleaseRequest 一致 */
    private record ReleaseRequest(Long orderId) {}
}
