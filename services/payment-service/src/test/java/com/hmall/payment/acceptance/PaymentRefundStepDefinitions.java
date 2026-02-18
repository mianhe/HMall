package com.hmall.payment.acceptance;

import com.hmall.payment.acceptance.config.LastResponseContext;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentRefundStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;

    public PaymentRefundStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
    }

    @当("以 orderId {string} 调用退款接口")
    public void 以OrderId调用退款接口(String orderId) {
        var response = restTemplate.postForEntity("/api/payments/refund",
            new HttpEntity<>(Map.of("orderId", Long.parseLong(orderId)), jsonHeaders()), Void.class);
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
    }

    @当("再次以 orderId {string} 调用退款接口")
    public void 再次以OrderId调用退款接口(String orderId) {
        以OrderId调用退款接口(orderId);
    }

    @当("以缺少 orderId 的请求 调用退款接口")
    public void 以缺少OrderId的请求调用退款接口() {
        var response = restTemplate.postForEntity("/api/payments/refund",
            new HttpEntity<>(Map.of(), jsonHeaders()), String.class);
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
    }

    @并且("按 orderId {string} 查询支付单 状态应为 {string}")
    public void 按OrderId查询支付单状态应为(String orderId, String expectedStatus) {
        var response = restTemplate.getForEntity("/api/payments/by-order/" + orderId, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"" + expectedStatus + "\"");
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
