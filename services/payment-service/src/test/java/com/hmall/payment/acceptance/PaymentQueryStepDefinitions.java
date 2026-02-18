package com.hmall.payment.acceptance;

import com.hmall.payment.acceptance.config.LastPaymentContext;
import com.hmall.payment.acceptance.config.LastResponseContext;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentQueryStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final LastPaymentContext lastPaymentContext;

    public PaymentQueryStepDefinitions(TestRestTemplate restTemplate,
                                       LastResponseContext lastResponseContext,
                                       LastPaymentContext lastPaymentContext) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.lastPaymentContext = lastPaymentContext;
    }

    @当("按 paymentId {string} 查询支付单")
    public void 按PaymentId查询支付单(String paymentIdKey) {
        long paymentId = "lastPaymentId".equals(paymentIdKey) && lastPaymentContext.getLastPaymentId() != null
            ? lastPaymentContext.getLastPaymentId()
            : Long.parseLong(paymentIdKey);
        var response = restTemplate.getForEntity("/api/payments/" + paymentId, String.class);
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
        lastResponseContext.setLastResponseBody(response.getBody());
    }

    @并且("响应包含 paymentId、orderId、amountCents {string}、status {string}、createdAt")
    public void 响应包含支付单详情(String amountCents, String status) {
        String body = lastResponseContext.getLastResponseBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"paymentId\"");
        assertThat(body).contains("\"orderId\"");
        assertThat(body).contains("\"amountCents\":" + amountCents);
        assertThat(body).contains("\"status\":\"" + status + "\"");
        assertThat(body).contains("\"createdAt\"");
    }
}
