package com.hmall.order.infrastructure.payment;

import com.hmall.order.application.port.RefundPaymentPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

/**
 * 通过 HTTP 调用 Payment 服务的 RefundPaymentPort 实现。
 * 契约见 docs/bounded-contexts/payment/api.yaml：POST /api/payments/refund。
 */
@Component
@ConditionalOnProperty(name = "payment.base-url")
public class RestRefundPaymentAdapter implements RefundPaymentPort {

    private final RestTemplate restTemplate;
    private final String refundUrl;

    public RestRefundPaymentAdapter(
            RestTemplate restTemplate,
            @Value("${payment.base-url}") String paymentBaseUrl) {
        this.restTemplate = restTemplate;
        this.refundUrl = paymentBaseUrl + "/api/payments/refund";
    }

    @Override
    public void refund(Long orderId) {
        RefundRequest request = new RefundRequest(orderId);
        try {
            restTemplate.exchange(
                    refundUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    Void.class);
        } catch (RestClientResponseException e) {
            throw new PaymentCallException("退款失败: " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            throw new PaymentUnavailableException("支付服务暂时不可用，请稍后重试", e);
        }
    }

    /** 与 api.yaml RefundRequest 一致 */
    private record RefundRequest(Long orderId) {}
}
