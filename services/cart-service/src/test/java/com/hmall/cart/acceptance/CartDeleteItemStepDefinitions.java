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

public class CartDeleteItemStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;

    public CartDeleteItemStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
        new ParameterizedTypeReference<>() {};

    @当("用户删除该购物车项")
    public void 用户删除该购物车项() {
        Long cartItemId = context.getLastCartItemId();
        deleteItem(cartItemId);
    }

    @当("用户删除购物车项 ID {long}")
    public void 用户删除购物车项ID(long cartItemId) {
        deleteItem(cartItemId);
    }

    @当("用户删除 SKU {long} 且 relatedSkuId 为空 的购物车项")
    public void 用户删除Sku且RelatedSkuId为空的购物车项(long skuId) {
        Long cartItemId = findCartItemIdBySkuAndRelatedSkuId(skuId, null);
        deleteItem(cartItemId);
    }

    @当("用户批量删除所有购物车项")
    public void 用户批量删除所有购物车项() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        Map<String, Object> body = Map.of("cartItemIds", context.getCartItemIds());
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cart/items",
                HttpMethod.DELETE,
                new HttpEntity<>(body, headers),
                Void.class
            );
            context.setLastStatusCode(response.getStatusCode().value());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
        }
    }

    private void deleteItem(Long cartItemId) {
        HttpHeaders headers = new HttpHeaders();
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                "/api/cart/items/" + cartItemId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Void.class
            );
            context.setLastStatusCode(response.getStatusCode().value());
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
