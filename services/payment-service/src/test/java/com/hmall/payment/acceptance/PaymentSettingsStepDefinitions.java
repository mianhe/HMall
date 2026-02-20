package com.hmall.payment.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.payment.acceptance.config.LastPaymentContext;
import com.hmall.payment.acceptance.config.LastResponseContext;
import com.hmall.payment.infrastructure.config.PaymentProperties;
import com.hmall.payment.infrastructure.config.PaymentSettingsInitializer;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentSettingsStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final LastPaymentContext lastPaymentContext;
    private final PaymentProperties paymentProperties;
    private final PaymentSettingsInitializer settingsInitializer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PaymentSettingsStepDefinitions(TestRestTemplate restTemplate,
                                          LastResponseContext lastResponseContext,
                                          LastPaymentContext lastPaymentContext,
                                          PaymentProperties paymentProperties,
                                          PaymentSettingsInitializer settingsInitializer) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.lastPaymentContext = lastPaymentContext;
        this.paymentProperties = paymentProperties;
        this.settingsInitializer = settingsInitializer;
    }

    @假如("管理员设置支付超时时间为 {int} 分钟")
    public void 管理员设置支付超时时间(int minutes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("expireMinutes", minutes);
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/payments/settings", HttpMethod.PUT,
                new HttpEntity<>(body, headers), String.class);
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
        lastResponseContext.setLastResponseBody(response.getBody());
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @当("支付设置从数据库重新加载")
    public void 支付设置从数据库重新加载() {
        paymentProperties.setExpireMinutes(30);
        settingsInitializer.run(null);
    }

    @那么("查询支付设置 超时时间应为 {int} 分钟")
    public void 查询支付设置超时时间应为(int expectedMinutes) {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/payments/settings", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"expireMinutes\":" + expectedMinutes);
    }

    @并且("按 paymentId {string} 查询支付单 expiredAt 应在 createdAt 之后约 {int} 分钟")
    public void 查询支付单expiredAt应在createdAt之后约N分钟(String paymentIdKey, int expectedMinutes) throws Exception {
        long paymentId = "lastPaymentId".equals(paymentIdKey) && lastPaymentContext.getLastPaymentId() != null
                ? lastPaymentContext.getLastPaymentId()
                : Long.parseLong(paymentIdKey);
        ResponseEntity<String> response = restTemplate.getForEntity("/api/payments/" + paymentId, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
        Instant createdAt = Instant.parse((String) json.get("createdAt"));
        Instant expiredAt = Instant.parse((String) json.get("expiredAt"));
        Duration diff = Duration.between(createdAt, expiredAt);

        long expectedSeconds = expectedMinutes * 60L;
        assertThat(diff.getSeconds()).isBetween(expectedSeconds - 2, expectedSeconds + 2);
    }
}
