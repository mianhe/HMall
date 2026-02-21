package com.hmall.order.infrastructure.fulfillment;

import com.hmall.order.application.port.CancelFulfillmentPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 HTTP 调用 Fulfillment 服务取消履约单。
 * 契约见 docs/bounded-contexts/fulfillment/api.yaml：POST /api/fulfillment/cancel。
 */
@Component
@ConditionalOnProperty(name = "fulfillment.base-url")
public class RestCancelFulfillmentAdapter implements CancelFulfillmentPort {

    private final RestTemplate restTemplate;
    private final String cancelUrl;

    public RestCancelFulfillmentAdapter(
            RestTemplate restTemplate,
            @Value("${fulfillment.base-url}") String fulfillmentBaseUrl) {
        this.restTemplate = restTemplate;
        this.cancelUrl = fulfillmentBaseUrl + "/api/fulfillment/cancel";
    }

    @Override
    public void cancelFulfillment(Long orderId) {
        CancelRequest request = new CancelRequest(orderId);
        try {
            restTemplate.exchange(
                    cancelUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class);
        } catch (RestClientResponseException e) {
            throw new FulfillmentCallException("取消履约单失败: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new FulfillmentUnavailableException("Fulfillment 服务暂时不可用，请稍后重试", e);
        }
    }

    private record CancelRequest(Long orderId) {}
}
