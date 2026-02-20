package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

public class CartDeleteItemStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;

    public CartDeleteItemStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("用户删除该购物车项")
    public void 用户删除该购物车项() {
        Long cartItemId = context.getLastCartItemId();
        deleteItem(cartItemId);
    }

    @当("用户删除购物车项 ID {long}")
    public void 用户删除购物车项ID(long cartItemId) {
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
}
