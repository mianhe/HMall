package com.hmall.cart.acceptance;

import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CartQueryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final CartTestContext context;
    private ResponseEntity<List<Map<String, Object>>> lastListResponse;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
        new ParameterizedTypeReference<>() {};

    public CartQueryStepDefinitions(TestRestTemplate restTemplate, CartTestContext context) {
        this.restTemplate = restTemplate;
        this.context = context;
    }

    @当("用户查询购物车")
    public void 用户查询购物车() {
        HttpHeaders headers = new HttpHeaders();
        if (context.getCurrentUserId() != null) {
            headers.set("X-User-Id", context.getCurrentUserId().toString());
        }
        try {
            lastListResponse = restTemplate.exchange(
                "/api/cart", HttpMethod.GET,
                new HttpEntity<>(headers), LIST_TYPE
            );
        } catch (RestClientResponseException e) {
            lastListResponse = ResponseEntity.status(e.getStatusCode()).body(null);
        }
        context.setLastStatusCode(lastListResponse.getStatusCode().value());
    }

    @并且("返回 {int} 个购物车项")
    public void 返回N个购物车项(int expectedCount) {
        assertThat(lastListResponse.getBody()).isNotNull();
        assertThat(lastListResponse.getBody()).hasSize(expectedCount);
    }

    @并且("SKU {long} 的购物车项标记为不可用")
    public void sku的购物车项标记为不可用(long skuId) {
        assertThat(lastListResponse.getBody()).isNotNull();
        Map<String, Object> item = lastListResponse.getBody().stream()
            .filter(m -> ((Number) m.get("skuId")).longValue() == skuId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("未找到 skuId=" + skuId + " 的购物车项"));
        assertThat(item.get("available")).isEqualTo(false);
    }
}
