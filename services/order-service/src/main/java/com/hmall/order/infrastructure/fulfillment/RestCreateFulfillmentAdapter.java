package com.hmall.order.infrastructure.fulfillment;

import com.hmall.order.application.port.CreateFulfillmentPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 通过 HTTP 调用 Fulfillment 服务创建履约单。
 * 契约见 docs/bounded-contexts/fulfillment/api.yaml：POST /api/fulfillment/create。
 */
@Component
@ConditionalOnProperty(name = "fulfillment.base-url")
public class RestCreateFulfillmentAdapter implements CreateFulfillmentPort {

    private final RestTemplate restTemplate;
    private final String createUrl;

    public RestCreateFulfillmentAdapter(
            RestTemplate restTemplate,
            @Value("${fulfillment.base-url}") String fulfillmentBaseUrl) {
        this.restTemplate = restTemplate;
        this.createUrl = fulfillmentBaseUrl + "/api/fulfillment/create";
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> createFulfillment(Long orderId, List<ItemQuantity> items,
                                         ShippingAddress shippingAddress) {
        CreateRequest request = new CreateRequest(
                orderId,
                items.stream()
                        .map(iq -> new ItemRequest(iq.skuId(), iq.quantity(), iq.itemType(), iq.relatedSkuId(), iq.serviceAttributes()))
                        .toList(),
                new AddressRequest(
                        shippingAddress.recipientName(), shippingAddress.phone(),
                        shippingAddress.province(), shippingAddress.city(),
                        shippingAddress.district(), shippingAddress.detail())
        );
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    createUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null || !body.containsKey("fulfillmentOrderIds")) {
                throw new FulfillmentCallException("Fulfillment 响应缺少 fulfillmentOrderIds");
            }
            List<Number> rawIds = (List<Number>) body.get("fulfillmentOrderIds");
            return rawIds.stream().map(Number::longValue).toList();
        } catch (RestClientResponseException e) {
            throw new FulfillmentCallException("创建履约单失败: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new FulfillmentUnavailableException("Fulfillment 服务暂时不可用，请稍后重试", e);
        }
    }

    private record CreateRequest(Long orderId, List<ItemRequest> items,
                                  AddressRequest shippingAddress) {}

    private record ItemRequest(long skuId, int quantity, String itemType, Long relatedSkuId, java.util.Map<String, Object> serviceAttributes) {}

    private record AddressRequest(String recipientName, String phone,
                                   String province, String city,
                                   String district, String detail) {}
}
