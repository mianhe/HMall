package com.hmall.payment.api;

import com.hmall.payment.application.PaymentApplicationService;
import com.hmall.payment.api.dto.PaymentDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 模拟支付页：供开发/联调使用。展示「支付成功」「支付失败」按钮，点击后调用回调并跳回前端。
 */
@RestController
@RequestMapping("/mock-pay")
public class MockPayController {

    private static final String DEFAULT_RETURN_URL = "http://localhost:5174/orders";

    private final PaymentApplicationService applicationService;

    public MockPayController(PaymentApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@RequestParam Long orderId,
                                      @RequestParam(required = false) String returnUrl) {
        PaymentDto payment;
        try {
            payment = applicationService.getByOrderId(orderId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "支付单不存在");
        }
        if (returnUrl == null || returnUrl.isBlank()) {
            returnUrl = DEFAULT_RETURN_URL;
        }
        String html = buildHtml(orderId, payment, returnUrl);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(MediaType.TEXT_HTML_VALUE))
            .body(html);
    }

    private String buildHtml(Long orderId, PaymentDto payment, String returnUrl) {
        long paymentId = payment.paymentId();
        String amountYuan = String.format("%.2f", (payment.amountCents() == null ? 0 : payment.amountCents()) / 100.0);
        String escReturn = returnUrl.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
        return """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"><title>模拟支付</title>
            <style>body{font-family:sans-serif;max-width:400px;margin:2rem auto;padding:1rem;}
            h1{color:#333;} .amount{font-size:1.5rem;color:#c00;} button{margin:0.5rem;padding:0.6rem 1.2rem;cursor:pointer;}
            .success{background:#0a0;color:#fff;border:none;border-radius:6px;}
            .fail{background:#666;color:#fff;border:none;border-radius:6px;}
            .back{background:#fff;color:#333;border:1px solid #ccc;border-radius:6px;}
            </style></head><body>
            <h1>模拟支付</h1>
            <p>订单号：%d</p>
            <p>支付金额：<span class="amount">¥ %s</span></p>
            <p>请选择：</p>
            <button class="success" onclick="submit(true)">支付成功</button>
            <button class="fail" onclick="submit(false)">支付失败</button>
            <br><br>
            <button class="back" onclick="location.href='%s'">返回订单列表</button>
            <script>
            const paymentId = %d;
            const returnUrl = "%s";
            async function submit(success) {
              const btn = event.target;
              btn.disabled = true;
              btn.textContent = '处理中…';
              await fetch("/api/payments/callback", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ paymentId, success })
              });
              btn.textContent = success ? '支付成功，跳转中…' : '已提交，跳转中…';
              setTimeout(() => { window.location.href = returnUrl; }, 1500);
            }
            </script>
            </body></html>
            """.formatted(orderId, amountYuan, escReturn, paymentId, escReturn);
    }
}
