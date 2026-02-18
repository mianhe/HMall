package com.hmall.payment.acceptance;

import com.hmall.payment.acceptance.config.LastPaymentContext;
import com.hmall.payment.acceptance.config.PaymentEventCapture;
import io.cucumber.java.zh_cn.当;
import io.cucumber.java.zh_cn.并且;
import io.cucumber.java.zh_cn.那么;
import io.cucumber.java.zh_cn.假如;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentExpireStepDefinitions {

    private final TestRestTemplate restTemplate;
    private final LastPaymentContext lastPaymentContext;
    private final PaymentEventCapture eventCapture;

    public PaymentExpireStepDefinitions(TestRestTemplate restTemplate,
                                        LastPaymentContext lastPaymentContext,
                                        PaymentEventCapture eventCapture) {
        this.restTemplate = restTemplate;
        this.lastPaymentContext = lastPaymentContext;
        this.eventCapture = eventCapture;
    }

    @当("执行超时检测")
    public void 执行超时检测() {
        restTemplate.postForEntity("/api/payments/run-expire-check", null, Void.class);
    }

    @那么("应发布恰好 {int} 次 PaymentExpired 事件 含 orderId {string}")
    public void 应发布恰好N次PaymentExpired事件含orderId(int count, String orderIdStr) {
        long orderId = Long.parseLong(orderIdStr);
        var list = eventCapture.getExpiredEvents();
        assertThat(list).hasSize(count);
        if (count > 0) {
            assertThat(list.get(0).orderId()).isEqualTo(orderId);
        }
    }

    @并且("按 paymentId {string} 查询支付单 状态应为 {string}")
    public void 按PaymentId查询支付单状态应为(String paymentIdKey, String expectedStatus) {
        long paymentId = "lastPaymentId".equals(paymentIdKey) && lastPaymentContext.getLastPaymentId() != null
            ? lastPaymentContext.getLastPaymentId()
            : Long.parseLong(paymentIdKey);
        var response = restTemplate.getForEntity("/api/payments/" + paymentId, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"" + expectedStatus + "\"");
    }

    @那么("应发布恰好 {int} 次 PaymentExpired 事件")
    public void 应发布恰好N次PaymentExpired事件(int count) {
        assertThat(eventCapture.getExpiredEvents()).hasSize(count);
    }
}
