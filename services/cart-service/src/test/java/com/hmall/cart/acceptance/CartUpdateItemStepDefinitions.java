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

public class CartUpdateItemStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;

    public CartUpdateItemStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("用户修改该购物车项数量为 {int}")
    public void 用户修改该购物车项数量为(int quantity) {
        Long cartItemId = context.getLastCartItemId();
        updateItem(cartItemId, quantity);
    }

    @当("用户修改购物车项 ID {long} 数量为 {int}")
    public void 用户修改购物车项ID数量为(long cartItemId, int quantity) {
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
}
