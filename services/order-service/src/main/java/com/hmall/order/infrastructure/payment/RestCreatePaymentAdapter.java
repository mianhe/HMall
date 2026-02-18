package com.hmall.order.infrastructure.payment;

import com.hmall.order.application.port.CreatePaymentPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 HTTP 调用 Payment 服务的 CreatePaymentPort 实现。
 * 契约见 docs/bounded-contexts/payment/api.yaml：POST /api/payments。
 */
@Component
@ConditionalOnProperty(name = "payment.base-url")
public class RestCreatePaymentAdapter implements CreatePaymentPort {

    private final RestTemplate restTemplate;
    private final String createPaymentUrl;

    public RestCreatePaymentAdapter(
            RestTemplate restTemplate,
            @Value("${payment.base-url}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.createPaymentUrl = paymentBaseUrl + "/api/payments";
    }

    @Override
    public void createPayment(Long orderId, long amountCents) {
        CreatePaymentRequest request = new CreatePaymentRequest(orderId, amountCents);
        try {
            restTemplate.exchange(
                    createPaymentUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class);
        } catch (RestClientResponseException e) {
            throw new PaymentCallException("创建支付失败: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new PaymentUnavailableException("支付服务暂时不可用，请稍后重试", e);
        }
    }

    /** 与 api.yaml CreatePaymentRequest 一致 */
    private record CreatePaymentRequest(Long orderId, long amountCents) {}
}
