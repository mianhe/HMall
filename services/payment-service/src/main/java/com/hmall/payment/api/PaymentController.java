package com.hmall.payment.api;

import com.hmall.payment.api.dto.CallbackRequestDto;
import com.hmall.payment.api.dto.CreatePaymentRequestDto;
import com.hmall.payment.api.dto.PaymentCreatedDto;
import com.hmall.payment.api.dto.PaymentDto;
import com.hmall.payment.api.dto.RefundRequestDto;
import com.hmall.payment.application.PaymentApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentApplicationService applicationService;

    public PaymentController(PaymentApplicationService applicationService) {
        this.applicationService = applicationService;
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

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getByPaymentId(@PathVariable Long paymentId) {
        return ResponseEntity.ok(applicationService.getByPaymentId(paymentId));
    }

    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<PaymentDto> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(applicationService.getByOrderId(orderId));
    }
}
