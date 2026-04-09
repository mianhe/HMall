package com.hmall.order.infrastructure.promotion;

import com.hmall.order.application.port.PromotionPricePort;
import com.hmall.order.domain.DiscountDetail;
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

@Component
@ConditionalOnProperty(name = "promotion.base-url")
public class RestPromotionPriceAdapter implements PromotionPricePort {

    private final RestTemplate restTemplate;
    private final String calculatePriceUrl;

    public RestPromotionPriceAdapter(
            RestTemplate restTemplate,
            @Value("${promotion.base-url}") String promotionBaseUrl) {
        this.restTemplate = restTemplate;
        this.calculatePriceUrl = promotionBaseUrl + "/api/promotion/calculate-price";
    }

    @Override
    public PriceResult calculatePrice(List<LineItem> items, Long userId, Long couponId) {
        var requestItems = items.stream()
                .map(i -> Map.of(
                        "skuId", (Object) i.skuId(),
                        "unitPriceCents", i.unitPriceCents(),
                        "quantity", i.quantity()))
                .toList();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("items", requestItems);
        body.put("userId", userId);
        if (couponId != null) {
            body.put("couponId", couponId);
        }

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    calculatePriceUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(body),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> result = resp.getBody();
            if (result == null) {
                throw new PromotionCallException("Promotion 计价返回空响应");
            }

            long originalAmountCents = toLong(result.get("originalAmountCents"));
            long discountAmountCents = toLong(result.get("discountAmountCents"));
            long payableAmountCents = toLong(result.get("payableAmountCents"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawLineItems = (List<Map<String, Object>>) result.get("lineItems");
            List<LineDiscount> lineDiscounts = rawLineItems == null ? List.of() : rawLineItems.stream().map(li -> {
                Long skuId = toLong(li.get("skuId"));
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawDiscounts = (List<Map<String, Object>>) li.get("discounts");
                List<DiscountDetail> discounts = rawDiscounts == null ? List.of() : rawDiscounts.stream()
                        .map(d -> new DiscountDetail(
                                (String) d.get("type"),
                                toLongOrNull(d.get("sourceId")),
                                toLong(d.get("amountCents")),
                                (String) d.get("description")))
                        .toList();
                return new LineDiscount(skuId, discounts);
            }).toList();

            return new PriceResult(originalAmountCents, discountAmountCents, payableAmountCents, lineDiscounts);
        } catch (RestClientResponseException e) {
            throw new PromotionCallException("Promotion 计价失败: " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new PromotionUnavailableException("Promotion 服务暂时不可用，请稍后重试", e);
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private static Long toLongOrNull(Object value) {
        if (value instanceof Number n) return n.longValue();
        return null;
    }
}
