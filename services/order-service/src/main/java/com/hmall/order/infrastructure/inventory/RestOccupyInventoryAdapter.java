package com.hmall.order.infrastructure.inventory;

import com.hmall.order.application.port.OccupyInventoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 通过 HTTP 调用 Inventory 服务的 OccupyInventoryPort 实现。
 * 契约见 docs/bounded-contexts/inventory/api.yaml：POST /api/inventory/occupy。
 */
@Component
@ConditionalOnProperty(name = "inventory.base-url")
public class RestOccupyInventoryAdapter implements OccupyInventoryPort {

    private final RestTemplate restTemplate;
    private final String occupyUrl;

    public RestOccupyInventoryAdapter(
            RestTemplate restTemplate,
            @Value("${inventory.base-url}") String inventoryBaseUrl) {
        this.restTemplate = restTemplate;
        this.occupyUrl = inventoryBaseUrl + "/api/inventory/occupy";
    }

    @Override
    public void occupy(Long orderId, List<ItemQuantity> items) {
        OccupyRequest request = new OccupyRequest(
                orderId,
                items.stream()
                        .map(iq -> new OccupyItemRequest(iq.skuId(), iq.quantity()))
                        .toList()
        );
        try {
            restTemplate.exchange(
                    occupyUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class);
        } catch (RestClientResponseException e) {
            throw new InventoryCallException("库存不足或参数错误: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new InventoryUnavailableException("库存服务暂时不可用，请稍后重试", e);
        }
    }

    /** 与 api.yaml OccupyRequest 一致 */
    private record OccupyRequest(Long orderId, List<OccupyItemRequest> items) {}

    private record OccupyItemRequest(Long skuId, Integer quantity) {}
}
