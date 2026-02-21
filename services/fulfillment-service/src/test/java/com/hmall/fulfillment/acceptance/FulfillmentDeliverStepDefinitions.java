package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentDelivered;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentDeliverStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final EventCapture eventCapture;

    public FulfillmentDeliverStepDefinitions(TestRestTemplate restTemplate,
                                             FulfillmentTestContext context,
                                             EventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.eventCapture = eventCapture;
    }

    @当("对该履约单确认签收")
    public void 对该履约单确认签收() {
        Long foId = context.getLastFulfillmentOrderId();
        postDeliver(foId);
    }

    @当("对不存在的履约单 ID {long} 确认签收")
    public void 对不存在的履约单确认签收(long foId) {
        postDeliver(foId);
    }

    @并且("应发布 FulfillmentDelivered 事件且 orderId 为 {long}")
    public void 应发布FulfillmentDelivered事件(long orderId) {
        List<FulfillmentDelivered> events = eventCapture.getDeliveredEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    private void postDeliver(Long foId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Void> res = restTemplate.exchange(
                "/api/fulfillment/" + foId + "/deliver",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class
            );
            context.setLastStatusCode(res.getStatusCode().value());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
        }
    }
}
