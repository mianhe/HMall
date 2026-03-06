package com.hmall.fulfillment.acceptance;

import com.hmall.fulfillment.domain.FulfillmentOrderRepository;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;

public class FulfillmentCompleteEngravingStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final FulfillmentTestContext context;
    private final FulfillmentOrderRepository repository;

    public FulfillmentCompleteEngravingStepDefinitions(TestRestTemplate restTemplate,
                                                       FulfillmentTestContext context,
                                                       FulfillmentOrderRepository repository) {
        this.restTemplate = restTemplate;
        this.context = context;
        this.repository = repository;
    }

    @当("对该履约单执行完成镭雕")
    public void 对该履约单执行完成镭雕() {
        Long foId = context.getLastFulfillmentOrderId();
        postCompleteEngraving(foId);
    }

    @当("对履约单 {long} 执行完成镭雕")
    public void 对履约单执行完成镭雕(long foId) {
        postCompleteEngraving(foId);
    }

    @并且("该履约单的 engravingCompletedAt 应已设置")
    public void 该履约单的engravingCompletedAt应已设置() {
        Long foId = context.getLastFulfillmentOrderId();
        var order = repository.findById(foId).orElseThrow();
        assertThat(order.getEngravingCompletedAt()).isNotNull();
    }

    private void postCompleteEngraving(Long foId) {
        try {
            ResponseEntity<Void> res = restTemplate.exchange(
                "/api/fulfillment/" + foId + "/complete-engraving",
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
