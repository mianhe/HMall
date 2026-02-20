package com.hmall.payment.api;

import com.hmall.payment.api.dto.CallbackRequestDto;
import com.hmall.payment.api.dto.CreatePaymentRequestDto;
import com.hmall.payment.api.dto.PaymentCreatedDto;
import com.hmall.payment.api.dto.PaymentDto;
import com.hmall.payment.api.dto.RefundRequestDto;
import com.hmall.payment.application.PaymentApplicationService;
import com.hmall.payment.infrastructure.config.PaymentProperties;
import com.hmall.payment.infrastructure.config.PaymentSettingsInitializer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentApplicationService applicationService;
    private final PaymentProperties paymentProperties;
    private final PaymentSettingsInitializer settingsInitializer;

    public PaymentController(PaymentApplicationService applicationService,
                             PaymentProperties paymentProperties,
                             PaymentSettingsInitializer settingsInitializer) {
        this.applicationService = applicationService;
        this.paymentProperties = paymentProperties;
        this.settingsInitializer = settingsInitializer;
    }

    @GetMapping
    public ResponseEntity<List<?>> list() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<PaymentCreatedDto> create(@Valid @RequestBody CreatePaymentRequestDto dto) {
        var result = applicationService.createPayment(dto.orderId(), dto.amountCents());
        return result.created()
            ? ResponseEntity.status(HttpStatus.CREATED).body(result.dto())
            : ResponseEntity.ok(result.dto());
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(@Valid @RequestBody CallbackRequestDto dto) {
        applicationService.handleCallback(dto.paymentId(), dto.success());
        return ResponseEntity.ok().build();
    }

    /** 执行超时检测（供定时任务或测试调用）。 */
    @PostMapping("/run-expire-check")
    public ResponseEntity<Void> runExpireCheck() {
        applicationService.runExpireCheck();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refund")
    public ResponseEntity<Void> refund(@Valid @RequestBody RefundRequestDto dto) {
        applicationService.refund(dto.orderId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(Map.of("expireMinutes", paymentProperties.getExpireMinutes()));
    }

    @PutMapping("/settings")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> body) {
        if (body.containsKey("expireMinutes")) {
            int val = ((Number) body.get("expireMinutes")).intValue();
            if (val < 1) {
                return ResponseEntity.badRequest().body(Map.of("message", "expireMinutes 须大于 0"));
            }
            paymentProperties.setExpireMinutes(val);
            settingsInitializer.persistExpireMinutes(val);
            int updated = applicationService.rescheduleAllPendingExpiredAt(val);
            return ResponseEntity.ok(Map.of("expireMinutes", paymentProperties.getExpireMinutes(),
                    "rescheduledPendingCount", updated));
        }
        return ResponseEntity.ok(Map.of("expireMinutes", paymentProperties.getExpireMinutes()));
    }

    @GetMapping("/{paymentId:\\d+}")
    public ResponseEntity<PaymentDto> getByPaymentId(@PathVariable Long paymentId) {
        return ResponseEntity.ok(applicationService.getByPaymentId(paymentId));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentDto> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(applicationService.getByOrderId(orderId));
    }
}
