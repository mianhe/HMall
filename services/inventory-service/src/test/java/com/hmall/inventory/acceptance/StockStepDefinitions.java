package com.hmall.inventory.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 库存管理 feature 的 Step Definitions，按 api.yaml 调用 API。
 */
public class StockStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext responseContext;

    public StockStepDefinitions(TestRestTemplate restTemplate, LastResponseContext responseContext) {
        this.restTemplate = restTemplate;
        this.responseContext = responseContext;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    @When("管理端 对 skuId {long} 设置可用库存为 {int}")
    public void 管理端设置可用库存(long skuId, int available) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
            baseUrl() + "/api/inventory/stock/" + skuId,
            HttpMethod.PUT,
            new HttpEntity<>(Map.of("available", available), headers),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        responseContext.setLastStatusCode(res.getStatusCode().value());
        responseContext.setLastStockBody(res.getBody());
    }

    @When("管理端 查询 skuId {long} 的库存")
    public void 管理端查询库存(long skuId) {
        ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
            baseUrl() + "/api/inventory/stock/" + skuId,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
        responseContext.setLastStatusCode(res.getStatusCode().value());
        responseContext.setLastStockBody(res.getBody());
    }

    @And("skuId {long} 的库存应为 available {int}、reserved {int}")
    public void skuId库存应为(long skuId, int expectedAvailable, int expectedReserved) {
        Map<String, Object> body = responseContext.getLastStockBody();
        assertThat(body).isNotNull();
        assertThat(((Number) body.get("skuId")).longValue()).isEqualTo(skuId);
        assertThat(((Number) body.get("available")).intValue()).isEqualTo(expectedAvailable);
        assertThat(((Number) body.get("reserved")).intValue()).isEqualTo(expectedReserved);
    }
}
