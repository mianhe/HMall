package com.hmall.payment.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.payment.acceptance.config.LastPaymentContext;
import com.hmall.payment.acceptance.config.LastResponseContext;
import com.hmall.payment.acceptance.config.PaymentEventCapture;
import com.hmall.payment.infrastructure.persistence.PaymentJpaRepository;
import io.cucumber.java.Before;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentCreateStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final LastPaymentContext lastPaymentContext;
    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentEventCapture eventCapture;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long firstPaymentIdForIdempotency;

    public PaymentCreateStepDefinitions(TestRestTemplate restTemplate, LastResponseContext lastResponseContext,
                                        LastPaymentContext lastPaymentContext,
                                        PaymentJpaRepository paymentJpaRepository,
                                        PaymentEventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.lastPaymentContext = lastPaymentContext;
        this.paymentJpaRepository = paymentJpaRepository;
        this.eventCapture = eventCapture;
    }

    @Before
    public void resetDatabaseAndEvents() {
        paymentJpaRepository.deleteAll();
        eventCapture.reset();
    }

    @假如("尚无订单 {string} 的支付单")
    public void 尚无订单的支付单(String orderId) {
        // 验收环境 H2 每次场景可视为干净；无需动作
    }

    @当("以订单 {string} 金额 {string} 分 调用创建支付接口")
    public void 以订单金额调用创建支付接口(String orderId, String amountCents) {
        Map<String, Object> body = Map.of(
            "orderId", Long.parseLong(orderId),
            "amountCents", Long.parseLong(amountCents)
        );
        postCreatePayment(body);
    }

    @当("以订单 {string} 金额 {string} 分 再次调用创建支付接口")
    public void 以订单金额再次调用创建支付接口(String orderId, String amountCents) {
        Map<String, Object> body = Map.of(
            "orderId", Long.parseLong(orderId),
            "amountCents", Long.parseLong(amountCents)
        );
        postCreatePayment(body);
    }

    @假如("已存在订单 {string} 的支付单 金额 {string} 分")
    public void 已存在订单的支付单金额(String orderId, String amountCents) {
        Map<String, Object> body = Map.of(
            "orderId", Long.parseLong(orderId),
            "amountCents", Long.parseLong(amountCents)
        );
        ResponseEntity<String> response = postCreatePayment(body);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
                Object pid = json.get("paymentId");
                if (pid instanceof Number) {
                    long id = ((Number) pid).longValue();
                    firstPaymentIdForIdempotency = id;
                    lastPaymentContext.setLastPaymentId(id);
                }
            } catch (Exception ignored) {}
        }
    }

    @当("以缺少 orderId 的请求 调用创建支付接口")
    public void 以缺少OrderId的请求调用创建支付接口() {
        Map<String, Object> body = Map.of("amountCents", 100L);
        postCreatePayment(body);
    }

    private ResponseEntity<String> postCreatePayment(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/payments",
            org.springframework.http.HttpMethod.POST,
            new HttpEntity<>(body, headers),
            String.class
        );
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
        lastResponseContext.setLastResponseBody(response.getBody());
        if (response.getBody() != null && response.getStatusCode().is2xxSuccessful()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = objectMapper.readValue(response.getBody(), Map.class);
                Object pid = json.get("paymentId");
                if (pid instanceof Number) {
                    lastPaymentContext.setLastPaymentId(((Number) pid).longValue());
                }
            } catch (Exception ignored) {}
        }
        return response;
    }

    @那么("应返回 201")
    public void 应返回201() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(201);
    }

    @那么("应返回成功（200 或 201）")
    public void 应返回成功200或201() {
        int code = lastResponseContext.getLastStatusCode();
        assertThat(code).isIn(200, 201);
    }

    @并且("响应包含 paymentId")
    public void 响应包含PaymentId() throws Exception {
        String body = lastResponseContext.getLastResponseBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(body, Map.class);
        assertThat(json).containsKey("paymentId");
        assertThat(json.get("paymentId")).isNotNull();
    }

    @并且("响应包含 payUrl")
    public void 响应包含PayUrl() throws Exception {
        String body = lastResponseContext.getLastResponseBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(body, Map.class);
        assertThat(json).containsKey("payUrl");
        assertThat(json.get("payUrl")).isNotNull();
    }

    @并且("响应中 status 为 {string}")
    public void 响应中Status为(String status) throws Exception {
        String body = lastResponseContext.getLastResponseBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(body, Map.class);
        assertThat(json.get("status")).isEqualTo(status);
    }

    @并且("返回的 paymentId 与首次创建时相同")
    public void 返回的PaymentId与首次创建时相同() throws Exception {
        assertThat(firstPaymentIdForIdempotency).isNotNull();
        String body = lastResponseContext.getLastResponseBody();
        assertThat(body).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = objectMapper.readValue(body, Map.class);
        Object pid = json.get("paymentId");
        assertThat(pid).isNotNull();
        assertThat(((Number) pid).longValue()).isEqualTo(firstPaymentIdForIdempotency);
    }

    @那么("应返回 400")
    public void 应返回400() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(400);
    }
}
