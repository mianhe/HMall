package com.hmall.payment.application;

import com.hmall.payment.api.dto.PaymentCreatedDto;
import com.hmall.payment.api.dto.PaymentDto;
import com.hmall.payment.application.event.PaymentCompletedEvent;
import com.hmall.payment.application.event.PaymentExpiredEvent;
import com.hmall.payment.application.event.PaymentFailedEvent;
import com.hmall.payment.application.port.PaymentDomainEventPublisher;
import com.hmall.payment.domain.Payment;
import com.hmall.payment.domain.PaymentRepository;
import com.hmall.payment.domain.PaymentStatus;
import com.hmall.payment.infrastructure.config.PaymentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class PaymentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentDomainEventPublisher eventPublisher;
    private final PaymentProperties paymentProperties;
    private final RestTemplate restTemplate;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                    PaymentDomainEventPublisher eventPublisher,
                                    PaymentProperties paymentProperties,
                                    RestTemplate restTemplate) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentProperties = paymentProperties;
        this.restTemplate = restTemplate;
    }

    /** @return 创建结果：dto 与是否为新创建（true=201，false=200 幂等） */
    @Transactional
    public CreatePaymentResult createPayment(Long orderId, Long amountCents) {
        if (orderId == null || amountCents == null || amountCents <= 0) {
            throw new PaymentBadRequestException("orderId 与 amountCents 必填且 amountCents 须大于 0");
        }
        var existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            Payment p = existing.get();
            return new CreatePaymentResult(
                new PaymentCreatedDto(
                    p.getPaymentId(),
                    p.getOrderId(),
                    p.getAmountCents(),
                    p.getStatus().name(),
                    p.getPayUrl()
                ),
                false
            );
        }
        int expireMinutes = paymentProperties.getExpireMinutes();
        String base = paymentProperties.getMockPayBaseUrl();
        if (base != null && !base.isEmpty()) {
            base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        }
        String payUrl = (base != null && !base.isEmpty())
            ? base + "/mock-pay?orderId=" + orderId
            : "";
        Payment payment = Payment.create(orderId, amountCents, payUrl, Instant.now(), expireMinutes);
        Payment saved = paymentRepository.save(payment);
        return new CreatePaymentResult(
            new PaymentCreatedDto(
                saved.getPaymentId(),
                saved.getOrderId(),
                saved.getAmountCents(),
                saved.getStatus().name(),
                saved.getPayUrl()
            ),
            true
        );
    }

    /** 处理网关回调。成功时置 COMPLETED 并发布 PaymentCompleted（重复成功幂等）；失败时置 FAILED 并发布 PaymentFailed。 */
    @Transactional
    public void handleCallback(Long paymentId, boolean success) {
        if (paymentId == null) {
            throw new PaymentBadRequestException("paymentId 必填");
        }
        Payment payment = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("支付单不存在"));
        Instant now = Instant.now();
        if (success) {
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                return;
            }
            payment.complete(now);
            paymentRepository.save(payment);
            eventPublisher.publish(new PaymentCompletedEvent(payment.getOrderId(), payment.getPaymentId(), now));
            notifyOrderPaymentCompleted(payment.getOrderId(), payment.getPaymentId());
        } else {
            payment.fail(now);
            paymentRepository.save(payment);
            eventPublisher.publish(new PaymentFailedEvent(payment.getOrderId(), now));
        }
    }

    /** 执行超时检测：将已过期的 PENDING 置为 EXPIRED 并发布 PaymentExpired。 */
    @Transactional
    public void runExpireCheck() {
        Instant now = Instant.now();
        List<Payment> toExpire = paymentRepository.findPendingWithExpiredAtBefore(now);
        for (Payment p : toExpire) {
            p.expire(now);
            paymentRepository.save(p);
            eventPublisher.publish(new PaymentExpiredEvent(p.getOrderId(), now));
        }
    }

    /** 退款。仅 COMPLETED 可退；同一 orderId 幂等。 */
    @Transactional
    public void refund(Long orderId) {
        if (orderId == null) {
            throw new PaymentBadRequestException("orderId 必填");
        }
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("支付单不存在"));
        try {
            Instant now = Instant.now();
            payment.refund(now);
            paymentRepository.save(payment);
        } catch (IllegalStateException e) {
            throw new PaymentBadRequestException(e.getMessage());
        }
    }

    /** 按 paymentId 查询支付单。 */
    public PaymentDto getByPaymentId(Long paymentId) {
        if (paymentId == null) {
            throw new PaymentBadRequestException("paymentId 必填");
        }
        Payment p = paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new IllegalArgumentException("支付单不存在"));
        return toDto(p);
    }

    /** 按 orderId 查询支付单。 */
    public PaymentDto getByOrderId(Long orderId) {
        if (orderId == null) {
            throw new PaymentBadRequestException("orderId 必填");
        }
        Payment p = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalArgumentException("支付单不存在"));
        return toDto(p);
    }

    private static PaymentDto toDto(Payment p) {
        return new PaymentDto(
            p.getPaymentId(),
            p.getOrderId(),
            p.getAmountCents(),
            p.getStatus().name(),
            p.getPayUrl(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            p.getExpiredAt()
        );
    }

    /** 通知 Order 服务支付完成（将订单置为 PAID）。 */
    private void notifyOrderPaymentCompleted(Long orderId, Long paymentId) {
        String base = paymentProperties.getOrderBaseUrl();
        if (base == null || base.isEmpty()) {
            return;
        }
        String url = (base.endsWith("/") ? base : base + "/") + "api/orders/internal/payment-completed";
        try {
            restTemplate.postForObject(url, Map.of("orderId", orderId, "paymentId", paymentId), Void.class);
        } catch (Exception e) {
            log.warn("通知 Order 支付完成失败: orderId={}, paymentId={}", orderId, paymentId, e);
        }
    }

    public record CreatePaymentResult(PaymentCreatedDto dto, boolean created) {}
}
