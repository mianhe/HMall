package com.hmall.order.infrastructure.promotion;

import com.hmall.order.application.port.CouponLifecyclePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "promotion.base-url")
public class RestCouponLifecycleAdapter implements CouponLifecyclePort {

    private final RestTemplate restTemplate;
    private final String couponsBaseUrl;

    public RestCouponLifecycleAdapter(
            RestTemplate restTemplate,
            @Value("${promotion.base-url}") String promotionBaseUrl) {
        this.restTemplate = restTemplate;
        this.couponsBaseUrl = promotionBaseUrl + "/api/promotion/coupons";
    }

    @Override
    public void lock(Long couponId, Long orderId) {
        String url = couponsBaseUrl + "/" + couponId + "/lock";
        Map<String, Object> body = Map.of("orderId", orderId);
        callPromotion(url, body);
    }

    @Override
    public void redeem(Long couponId) {
        String url = couponsBaseUrl + "/" + couponId + "/redeem";
        callPromotion(url, null);
    }

    @Override
    public void release(Long couponId) {
        String url = couponsBaseUrl + "/" + couponId + "/release";
        callPromotion(url, null);
    }

    private void callPromotion(String url, Object body) {
        try {
            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body), Void.class);
        } catch (RestClientResponseException e) {
            throw new PromotionCallException("Promotion 券操作失败: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new PromotionUnavailableException("Promotion 服务暂时不可用，请稍后重试", e);
        }
    }
}
