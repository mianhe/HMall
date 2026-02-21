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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentDomainEventPublisher eventPublisher;
    private final PaymentProperties paymentProperties;

    public PaymentApplicationService(PaymentRepository paymentRepository,
                                    PaymentDomainEventPublisher eventPublisher,
                                    PaymentProperties paymentProperties) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.paymentProperties = paymentProperties;
    }

    /** @return 创建结果：dto 与是否为新创建（true=201，false=200 幂等） */
    @Transactional
    public CreatePaymentResult createPayment(Long orderId, Long amountCents) {
        if (orderId == null || amountCents == null || amountCents <= 0) {
            throw new PaymentBadRequestException("orderId 与 amountCents 必填且 amountCents 须大于 0");
        }
        var existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return new CreatePaymentResult(toCreatedDto(existing.get()), false);
        }
        String payUrl = buildPayUrl(orderId);
        int expireMinutes = paymentProperties.getExpireMinutes();
        Payment payment = Payment.create(orderId, amountCents, payUrl, Instant.now(), expireMinutes);
        Payment saved = paymentRepository.save(payment);
        return new CreatePaymentResult(toCreatedDto(saved), true);
    }

    /**
     * 处理网关回调。成功时置 COMPLETED 并发布 PaymentCompleted（重复成功幂等）；
     * 失败时不改变支付单状态（保持 PENDING，用户可重试），仅发布 PaymentFailed 通知。
     */
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
            eventPublisher.publish(new PaymentCompletedEvent(payment.getOrderId(), payment.getPaymentId(), payment.getAmountCents(), now));
        } else {
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

    /**
     * 将所有 PENDING 支付单的 expiredAt 按 expireMinutes 重新计算（createdAt + expireMinutes 分钟）。
     * 用于管理员修改超时配置后立即生效到已有订单。
     */
    @Transactional
    public int rescheduleAllPendingExpiredAt(int expireMinutes) {
        List<Payment> pending = paymentRepository.findAllPending();
        for (Payment p : pending) {
            Instant newExpiredAt = p.getCreatedAt().plusSeconds(expireMinutes * 60L);
            Payment rescheduled = Payment.reconstitute(
                p.getPaymentId(), p.getOrderId(), p.getAmountCents(),
                p.getStatus(), p.getPayUrl(),
                p.getCreatedAt(), p.getUpdatedAt(), newExpiredAt
            );
            paymentRepository.save(rescheduled);
        }
        return pending.size();
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

    private String buildPayUrl(Long orderId) {
        String base = paymentProperties.getMockPayBaseUrl();
        if (base == null || base.isBlank()) return "";
        base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return base + "/mock-pay?orderId=" + orderId;
    }

    private static PaymentCreatedDto toCreatedDto(Payment p) {
        return new PaymentCreatedDto(
            p.getPaymentId(), p.getOrderId(), p.getAmountCents(),
            p.getStatus().name(), p.getPayUrl()
        );
    }

    private static PaymentDto toDto(Payment p) {
        return new PaymentDto(
            p.getPaymentId(), p.getOrderId(), p.getAmountCents(),
            p.getStatus().name(), p.getPayUrl(),
            p.getCreatedAt(), p.getUpdatedAt(), p.getExpiredAt()
        );
    }

    public record CreatePaymentResult(PaymentCreatedDto dto, boolean created) {}
}
