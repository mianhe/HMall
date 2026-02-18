package com.hmall.payment.acceptance;

import com.hmall.payment.acceptance.config.LastPaymentContext;
import com.hmall.payment.acceptance.config.LastResponseContext;
import com.hmall.payment.acceptance.config.PaymentEventCapture;
import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import io.cucumber.java.zh_cn.假如;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentCallbackStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastResponseContext lastResponseContext;
    private final LastPaymentContext lastPaymentContext;
    private final PaymentEventCapture eventCapture;

    public PaymentCallbackStepDefinitions(TestRestTemplate restTemplate,
                                          LastResponseContext lastResponseContext,
                                          LastPaymentContext lastPaymentContext,
                                          PaymentEventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.lastResponseContext = lastResponseContext;
        this.lastPaymentContext = lastPaymentContext;
        this.eventCapture = eventCapture;
    }

    @并且("记录该支付单的 paymentId 为 {string}")
    public void 记录该支付单的paymentId为(String name) {
        // 已在创建步骤中写入 LastPaymentContext
    }

    @当("以支付成功 回调 paymentId {string}")
    public void 以支付成功回调PaymentId(String paymentIdKey) {
        long paymentId = resolvePaymentId(paymentIdKey);
        postCallback(paymentId, true);
    }

    @当("以支付失败 回调 paymentId {string}")
    public void 以支付失败回调PaymentId(String paymentIdKey) {
        long paymentId = resolvePaymentId(paymentIdKey);
        postCallback(paymentId, false);
    }

    @并且("再次以支付成功 回调 paymentId {string}")
    public void 再次以支付成功回调PaymentId(String paymentIdKey) {
        long paymentId = resolvePaymentId(paymentIdKey);
        postCallback(paymentId, true);
    }

    private void postCallback(long paymentId, boolean success) {
        Map<String, Object> body = Map.of("paymentId", paymentId, "success", success);
        var response = restTemplate.postForEntity("/api/payments/callback", new HttpEntity<>(body, jsonHeaders()), Void.class);
        lastResponseContext.setLastStatusCode(response.getStatusCode().value());
    }

    private long resolvePaymentId(String key) {
        if ("lastPaymentId".equals(key) && lastPaymentContext.getLastPaymentId() != null) {
            return lastPaymentContext.getLastPaymentId();
        }
        return Long.parseLong(key);
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @那么("应返回 200")
    public void 应返回200() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(200);
    }

    @那么("应返回 404")
    public void 应返回404() {
        assertThat(lastResponseContext.getLastStatusCode()).isEqualTo(404);
    }

    @并且("应发布恰好 {int} 次 PaymentCompleted 事件 含 orderId {string} 与 paymentId {string}")
    public void 应发布恰好N次PaymentCompleted事件含orderId与paymentId(int count, String orderIdStr, String paymentIdKey) {
        long orderId = Long.parseLong(orderIdStr);
        Long paymentId = "lastPaymentId".equals(paymentIdKey) ? lastPaymentContext.getLastPaymentId() : Long.parseLong(paymentIdKey);
        assertThat(paymentId).isNotNull();
        var list = eventCapture.getCompletedEvents();
        assertThat(list).hasSize(count);
        if (count > 0) {
            assertThat(list.get(0).orderId()).isEqualTo(orderId);
            assertThat(list.get(0).paymentId()).isEqualTo(paymentId);
        }
    }

    @并且("应发布恰好 {int} 次 PaymentFailed 事件 含 orderId {string}")
    public void 应发布恰好N次PaymentFailed事件含orderId(int count, String orderIdStr) {
        long orderId = Long.parseLong(orderIdStr);
        var list = eventCapture.getFailedEvents();
        assertThat(list).hasSize(count);
        if (count > 0) {
            assertThat(list.get(0).orderId()).isEqualTo(orderId);
        }
    }

    @并且("应发布恰好 {int} 次 PaymentCompleted 事件 含 orderId {string}")
    public void 应发布恰好N次PaymentCompleted事件含orderId(int count, String orderIdStr) {
        long orderId = Long.parseLong(orderIdStr);
        var list = eventCapture.getCompletedEvents();
        assertThat(list).hasSize(count);
        if (count > 0) {
            assertThat(list.get(0).orderId()).isEqualTo(orderId);
        }
    }
}
