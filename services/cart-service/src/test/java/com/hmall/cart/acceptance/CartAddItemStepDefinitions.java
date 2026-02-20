package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CartAddItemStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;

    public CartAddItemStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("用户添加 SKU {long} 数量 {int} 到购物车")
    public void 用户添加SKU到购物车(long skuId, int quantity) {
        ResponseEntity<Map> response = postAddItem(skuId, quantity);
        context.setLastStatusCode(response.getStatusCode().value());
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            context.setLastCartItemResponse(response.getBody());
            Number cartItemId = (Number) response.getBody().get("cartItemId");
            if (cartItemId != null) {
                context.setLastCartItemId(cartItemId.longValue());
                context.addCartItemId(cartItemId.longValue());
            }
        }
    }

    @假如("用户已添加 SKU {long} 数量 {int} 到购物车")
    public void 用户已添加SKU到购物车(long skuId, int quantity) {
        用户添加SKU到购物车(skuId, quantity);
        assertThat(context.getLastStatusCode()).isEqualTo(200);
    }

    @并且("返回的购物车项 skuId 为 {long}")
    public void 返回的购物车项skuId为(long expectedSkuId) {
        Map<String, Object> body = context.getLastCartItemResponse();
        assertThat(body).isNotNull();
        Number skuId = (Number) body.get("skuId");
        assertThat(skuId.longValue()).isEqualTo(expectedSkuId);
    }

    @并且("返回的购物车项数量为 {int}")
    public void 返回的购物车项数量为(int expectedQuantity) {
        Map<String, Object> body = context.getLastCartItemResponse();
        assertThat(body).isNotNull();
        Number quantity = (Number) body.get("quantity");
        assertThat(quantity.intValue()).isEqualTo(expectedQuantity);
    }

    private ResponseEntity<Map> postAddItem(long skuId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        Map<String, Object> body = Map.of("skuId", skuId, "quantity", quantity);
        try {
            return restTemplate.postForEntity(
                "/api/cart/items",
                new HttpEntity<>(body, headers),
                Map.class
            );
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }
}
