package com.hmall.inventory.acceptance;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
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
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                baseUrl() + "/api/inventory/stock/" + skuId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("available", available), headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            responseContext.setLastStatusCode(res.getStatusCode().value());
            responseContext.setLastStockBody(res.getBody());
        } catch (RestClientResponseException e) {
            responseContext.setLastStatusCode(e.getStatusCode().value());
        }
    }

    @When("管理端 查询 skuId {long} 的库存")
    public void 管理端查询库存(long skuId) {
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                baseUrl() + "/api/inventory/stock/" + skuId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            responseContext.setLastStatusCode(res.getStatusCode().value());
            responseContext.setLastStockBody(res.getBody());
        } catch (RestClientResponseException e) {
            responseContext.setLastStatusCode(e.getStatusCode().value());
        }
    }

    @Then("应返回 {int}")
    public void 应返回(int expectedStatus) {
        assertThat(responseContext.getLastStatusCode()).isEqualTo(expectedStatus);
    }

    @And("skuId {long} 的库存应为 available {int}、reserved {int}")
    public void skuId库存应为(long skuId, int expectedAvailable, int expectedReserved) {
        Map<String, Object> body = responseContext.getLastStockBody();
        assertThat(body).isNotNull();
        assertThat(((Number) body.get("skuId")).longValue()).isEqualTo(skuId);
        assertThat(((Number) body.get("available")).intValue()).isEqualTo(expectedAvailable);
        assertThat(((Number) body.get("reserved")).intValue()).isEqualTo(expectedReserved);
    }

    @When("管理端 查询全部库存列表")
    public void 管理端查询全部库存列表() {
        ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
            baseUrl() + "/api/inventory/stock",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        responseContext.setLastStatusCode(res.getStatusCode().value());
        responseContext.setLastStockListBody(res.getBody());
    }

    @When("管理端 批量查询 skuId 列表 {long}、{long}、{long} 的库存")
    public void 管理端批量查询库存(long id1, long id2, long id3) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<List<Map<String, Object>>> res = restTemplate.exchange(
            baseUrl() + "/api/inventory/stock/batch",
            HttpMethod.POST,
            new HttpEntity<>(List.of(id1, id2, id3), headers),
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        responseContext.setLastStatusCode(res.getStatusCode().value());
        responseContext.setLastStockListBody(res.getBody());
    }

    @And("返回的库存列表应包含 skuId {long} 且 available {int}")
    public void 库存列表应包含(long skuId, int expectedAvailable) {
        List<Map<String, Object>> list = responseContext.getLastStockListBody();
        assertThat(list).isNotNull();
        assertThat(list).anySatisfy(item -> {
            assertThat(((Number) item.get("skuId")).longValue()).isEqualTo(skuId);
            assertThat(((Number) item.get("available")).intValue()).isEqualTo(expectedAvailable);
        });
    }

    @And("返回的库存列表长度应为 {int}")
    public void 库存列表长度应为(int expectedSize) {
        List<Map<String, Object>> list = responseContext.getLastStockListBody();
        assertThat(list).isNotNull().hasSize(expectedSize);
    }
}
