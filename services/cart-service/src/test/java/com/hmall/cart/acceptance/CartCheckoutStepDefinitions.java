package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CartCheckoutStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;
    private ResponseEntity<Map> lastResponse;

    public CartCheckoutStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("用户勾选所有购物车项进行结算预览")
    public void 用户勾选所有购物车项进行结算预览() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        Map<String, Object> body = Map.of("cartItemIds", context.getCartItemIds());
        try {
            lastResponse = restTemplate.postForEntity(
                "/api/cart/checkout-preview",
                new HttpEntity<>(body, headers),
                Map.class
            );
        } catch (RestClientResponseException e) {
            lastResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastResponse.getStatusCode().value());
    }

    @并且("结算预览包含 {int} 个商品项")
    public void 结算预览包含N个商品项(int expectedCount) {
        assertThat(lastResponse.getBody()).isNotNull();
        List<?> items = (List<?>) lastResponse.getBody().get("items");
        assertThat(items).hasSize(expectedCount);
    }

    @并且("结算预览总价为 {int}")
    public void 结算预览总价为(int expectedTotal) {
        assertThat(lastResponse.getBody()).isNotNull();
        Number totalPrice = (Number) lastResponse.getBody().get("totalPrice");
        assertThat(totalPrice.intValue()).isEqualTo(expectedTotal);
    }
}
