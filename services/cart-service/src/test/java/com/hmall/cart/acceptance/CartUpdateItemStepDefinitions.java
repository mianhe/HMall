package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.List;

public class CartUpdateItemStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;

    public CartUpdateItemStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
        new ParameterizedTypeReference<>() {};

    @当("用户修改该购物车项数量为 {int}")
    public void 用户修改该购物车项数量为(int quantity) {
        Long cartItemId = context.getLastCartItemId();
        updateItem(cartItemId, quantity);
    }

    @当("用户修改购物车项 ID {long} 数量为 {int}")
    public void 用户修改购物车项ID数量为(long cartItemId, int quantity) {
        updateItem(cartItemId, quantity);
    }

    @当("用户将 SKU {long} 且 relatedSkuId 为空 的购物车项数量修改为 {int}")
    public void 用户将Sku且RelatedSkuId为空的购物车项数量修改为(long skuId, int quantity) {
        Long cartItemId = findCartItemIdBySkuAndRelatedSkuId(skuId, null);
        updateItem(cartItemId, quantity);
    }

    private void updateItem(Long cartItemId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        Map<String, Object> body = Map.of("quantity", quantity);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                "/api/cart/items/" + cartItemId,
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Map.class
            );
            context.setLastStatusCode(response.getStatusCode().value());
            if (response.getBody() != null) {
                context.setLastCartItemResponse(response.getBody());
            }
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
        }
    }

    private Long findCartItemIdBySkuAndRelatedSkuId(long skuId, Long relatedSkuId) {
        HttpHeaders headers = new HttpHeaders();
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            "/api/cart",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            LIST_TYPE
        );
        List<Map<String, Object>> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("购物车为空");
        }
        return body.stream()
            .filter(item -> ((Number) item.get("skuId")).longValue() == skuId)
            .filter(item -> {
                Number related = (Number) item.get("relatedSkuId");
                if (relatedSkuId == null) {
                    return related == null;
                }
                return related != null && related.longValue() == relatedSkuId;
            })
            .map(item -> ((Number) item.get("cartItemId")).longValue())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("未找到匹配的购物车项"));
    }
}
