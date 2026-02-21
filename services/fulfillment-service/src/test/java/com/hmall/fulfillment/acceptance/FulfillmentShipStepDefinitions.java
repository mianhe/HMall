package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentShipped;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
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

public class FulfillmentShipStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final EventCapture eventCapture;

    public FulfillmentShipStepDefinitions(TestRestTemplate restTemplate,
                                          FulfillmentTestContext context,
                                          EventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.eventCapture = eventCapture;
    }

    @当("对该履约单执行发货 承运商 {string} 物流单号 {string}")
    public void 对该履约单执行发货(String carrier, String trackingNumber) {
        Long foId = context.getLastFulfillmentOrderId();
        postShip(foId, carrier, trackingNumber);
    }

    @当("对不存在的履约单 ID {long} 执行发货 承运商 {string} 物流单号 {string}")
    public void 对不存在的履约单执行发货(long foId, String carrier, String trackingNumber) {
        postShip(foId, carrier, trackingNumber);
    }

    @并且("应发布 FulfillmentShipped 事件且 orderId 为 {long}")
    public void 应发布FulfillmentShipped事件(long orderId) {
        List<FulfillmentShipped> events = eventCapture.getShippedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    private void postShip(Long foId, String carrier, String trackingNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("carrier", carrier, "trackingNumber", trackingNumber);
        try {
            ResponseEntity<Void> res = restTemplate.exchange(
                "/api/fulfillment/" + foId + "/ship",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Void.class
            );
            context.setLastStatusCode(res.getStatusCode().value());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
        }
    }
}
