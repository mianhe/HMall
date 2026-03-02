package com.hmall.inventory.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.inventory.api.dto.OccupyItemDto;
import com.hmall.inventory.api.dto.OccupyRequestDto;
import com.hmall.inventory.domain.SkuStock;
import com.hmall.inventory.domain.SkuStockRepository;
import com.hmall.inventory.domain.StockReserved;
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
 * 占用库存 feature 的 Step Definitions，按 api.yaml 调用 API。
 */
public class OccupyStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext responseContext;
    private final SkuStockRepository skuStockRepository;
    private final EventCapture eventCapture;

    private ResponseEntity<Map<String, Object>> lastOccupyResponse;

    public OccupyStepDefinitions(TestRestTemplate restTemplate,
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

    @Given("skuId {long} 可用库存为 {int}")
    public void skuId可用库存为(long skuId, int available) {
        skuStockRepository.save(new SkuStock(skuId, available, 0));
    }

    @When("Order 调用占用接口 orderId {long} 且 items 为 skuId {long} 数量 {int}")
    public void order调用占用接口单个item(long orderId, long skuId, int quantity) {
        List<OccupyItemDto> items = List.of(new OccupyItemDto(skuId, quantity));
        postOccupy(orderId, items);
    }

    @When("Order 调用占用接口 orderId {long} 且 items 为 skuId {long} 数量 {int}、skuId {long} 数量 {int}")
    public void order调用占用接口两个items(long orderId, long sku1, int qty1, long sku2, int qty2) {
        List<OccupyItemDto> items = List.of(
            new OccupyItemDto(sku1, qty1),
            new OccupyItemDto(sku2, qty2)
        );
        postOccupy(orderId, items);
    }

    @When("Order 再次调用占用接口 orderId {long} 且 items 为 skuId {long} 数量 {int}")
    public void order再次调用占用接口(long orderId, long skuId, int quantity) {
        List<OccupyItemDto> items = List.of(new OccupyItemDto(skuId, quantity));
        postOccupy(orderId, items);
    }

    @When("Order 调用占用接口且 request body 为缺少 orderId")
    public void order调用占用接口缺少orderId() {
        Map<String, Object> body = Map.of("items", List.of(Map.of("skuId", 1, "quantity", 1)));
        postOccupyBody(body);
    }

    @When("Order 调用占用接口且 request body 为缺少 items")
    public void order调用占用接口缺少items() {
        Map<String, Object> body = Map.of("orderId", 1L);
        postOccupyBody(body);
    }

    @Then("占用应成功")
    public void 占用应成功() {
        assertThat(responseContext.getLastStatusCode()).isEqualTo(200);
    }

    @Then("占用应失败")
    public void 占用应失败() {
        assertThat(responseContext.getLastStatusCode()).isGreaterThanOrEqualTo(400);
    }


    @And("错误信息包含 {string}")
    public void 错误信息包含(String expected) {
        assertThat(lastOccupyResponse).isNotNull();
        assertThat(lastOccupyResponse.getBody()).isNotNull();
        Object message = lastOccupyResponse.getBody().get("message");
        assertThat(message).asString().contains(expected);
    }

    @And("应发布 StockReserved 事件且 orderId 为 {long}")
    public void 应发布StockReserved事件且orderId为(long orderId) {
        List<StockReserved> events = eventCapture.getStockReservedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    private void postOccupy(Long orderId, List<OccupyItemDto> items) {
        postOccupyRequest(new OccupyRequestDto(orderId, items));
    }

    private void postOccupyBody(Map<String, Object> body) {
        postOccupyRequest(body);
    }

    private void postOccupyRequest(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map<String, Object>> res = restTemplate.exchange(
                baseUrl() + "/api/inventory/occupy",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            responseContext.setLastStatusCode(res.getStatusCode().value());
            lastOccupyResponse = res;
        } catch (RestClientResponseException e) {
            responseContext.setLastStatusCode(e.getStatusCode().value());
            lastOccupyResponse = ResponseEntity.status(e.getStatusCode()).body(parseErrorBody(e));
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseErrorBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return new ObjectMapper().readValue(body, Map.class);
        } catch (Exception ignored) {
            return Map.of("message", body);
        }
    }
}
