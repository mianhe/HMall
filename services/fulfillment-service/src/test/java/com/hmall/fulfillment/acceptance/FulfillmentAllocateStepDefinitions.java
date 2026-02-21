package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentOrderAllocated;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentAllocateStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final EventCapture eventCapture;

    public FulfillmentAllocateStepDefinitions(TestRestTemplate restTemplate,
                                              FulfillmentTestContext context,
                                              EventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.eventCapture = eventCapture;
    }

    @当("对该履约单执行开始配货")
    public void 对该履约单执行开始配货() {
        Long foId = context.getLastFulfillmentOrderId();
        postAllocate(foId);
    }

    @当("对不存在的履约单 ID {long} 执行开始配货")
    public void 对不存在的履约单执行开始配货(long foId) {
        postAllocate(foId);
    }

    @并且("应发布 FulfillmentOrderAllocated 事件且 orderId 为 {long}")
    public void 应发布FulfillmentOrderAllocated事件(long orderId) {
        var events = eventCapture.getAllocatedEvents();
        assertThat(events).isNotEmpty();
        assertThat(events.get(events.size() - 1).orderId()).isEqualTo(orderId);
    }

    private void postAllocate(Long foId) {
        try {
            ResponseEntity<Void> res = restTemplate.exchange(
                "/api/fulfillment/" + foId + "/allocate",
                HttpMethod.POST,
                null,
                Void.class
            );
            context.setLastStatusCode(res.getStatusCode().value());
        } catch (RestClientResponseException e) {
            context.setLastStatusCode(e.getStatusCode().value());
        }
    }
}
