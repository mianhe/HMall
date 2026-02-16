package com.hmall.inventory.acceptance;

import com.hmall.inventory.api.dto.OccupyItemDto;
import com.hmall.inventory.api.dto.OccupyRequestDto;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.domain.StockReleased;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
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
 * 释放库存 feature 的 Step Definitions，按 api.yaml 调用 API。
 */
public class ReleaseStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext responseContext;
    private final SkuStockRepository skuStockRepository;
    private final EventCapture eventCapture;

    public ReleaseStepDefinitions(TestRestTemplate restTemplate,
                                  LastResponseContext responseContext,
                                  SkuStockRepository skuStockRepository,
                                  EventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.responseContext = responseContext;
        this.skuStockRepository = skuStockRepository;
        this.eventCapture = eventCapture;
    }

    private String baseUrl() {
        return restTemplate.getRootUri().toString();
    }

    @Given("Order 已占用 orderId {long} 且 items 为 skuId {long} 数量 {int}")
    public void order已占用(long orderId, long skuId, int quantity) {
        List<OccupyItemDto> items = List.of(new OccupyItemDto(skuId, quantity));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange(
            baseUrl() + "/api/inventory/occupy",
            HttpMethod.POST,
            new HttpEntity<>(new OccupyRequestDto(orderId, items), headers),
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );
    }

    @When("Order 调用释放接口 orderId {long}")
    public void order调用释放接口(long orderId) {
        postRelease(Map.of("orderId", orderId));
    }

    @When("Order 调用释放接口且 request body 为缺少 orderId")
    public void order调用释放接口缺少orderId() {
        postRelease(Map.of());
    }

    @Then("释放应成功")
    public void 释放应成功() {
        assertThat(responseContext.getLastStatusCode()).isEqualTo(200);
    }

    @And("skuId {long} 可用库存应为 {int}")
    public void skuId可用库存应为(long skuId, int expectedAvailable) {
        int available = skuStockRepository.findBySkuId(skuId)
            .map(s -> s.getAvailable())
            .orElse(-1);
        assertThat(available).isEqualTo(expectedAvailable);
    }

    @And("应发布 StockReleased 事件且 orderId 为 {long}")
    public void 应发布StockReleased事件且orderId为(long orderId) {
        List<StockReleased> events = eventCapture.getStockReleasedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    private void postRelease(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                baseUrl() + "/api/inventory/release",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            responseContext.setLastStatusCode(res.getStatusCode().value());
        } catch (RestClientResponseException e) {
            responseContext.setLastStatusCode(e.getStatusCode().value());
        }
    }
}
