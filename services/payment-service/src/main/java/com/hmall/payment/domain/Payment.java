package com.hmall.payment.domain;

import java.time.Instant;

/**
 * 支付单聚合根。创建后为 PENDING；由网关回调或超时检测驱动状态变更。
 */
public class Payment {

    private Long paymentId;  // 新建时为 null，持久化后由仓储填充
    private final Long orderId;
    private final Long amountCents;
    private PaymentStatus status;
    private final String payUrl;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Instant expiredAt;

    private Payment(Long orderId, Long amountCents, PaymentStatus status, String payUrl,
                    Instant createdAt, Instant updatedAt, Instant expiredAt) {
        this.paymentId = null;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.status = status;
        this.payUrl = payUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiredAt = expiredAt;
    }

    private Payment(Long paymentId, Long orderId, Long amountCents, PaymentStatus status, String payUrl,
                    Instant createdAt, Instant updatedAt, Instant expiredAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.status = status;
        this.payUrl = payUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiredAt = expiredAt;
    }

    public static Payment create(Long orderId, Long amountCents, String payUrl, Instant now, int expireMinutes) {
        if (orderId == null || amountCents == null || amountCents <= 0) {
            throw new IllegalArgumentException("orderId 与 amountCents 必填且 amountCents 须大于 0");
        }
        Instant expiredAt = now.plusSeconds(expireMinutes * 60L);
        return new Payment(orderId, amountCents, PaymentStatus.PENDING, payUrl, now, now, expiredAt);
    }

    /** 从持久化重建（用于仓储查询） */
    public static Payment reconstitute(Long paymentId, Long orderId, Long amountCents, PaymentStatus status, String payUrl,
                                       Instant createdAt, Instant updatedAt, Instant expiredAt) {
        return new Payment(paymentId, orderId, amountCents, status, payUrl, createdAt, updatedAt, expiredAt);
    }

    /** 支付成功。仅 PENDING 可执行；已 COMPLETED 时幂等（不抛错）。 */
    public void complete(Instant now) {
        if (status == PaymentStatus.COMPLETED) {
            return;
        }
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("仅 PENDING 支付单可置为 COMPLETED，当前: " + status);
        }
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = now;
    }

    /** 超时过期。仅 PENDING 且 now >= expiredAt 可执行。 */
    public void expire(Instant now) {
        if (status != PaymentStatus.PENDING) {
            return;
        }
        if (now.isBefore(expiredAt)) {
            return;
        }
        this.status = PaymentStatus.EXPIRED;
        this.updatedAt = now;
    }

    /** 退款。仅 COMPLETED 可执行；已 REFUNDED 时幂等。 */
    public void refund(Instant now) {
        if (status == PaymentStatus.REFUNDED) {
            return;
        }
        if (status != PaymentStatus.COMPLETED) {
            throw new IllegalStateException("仅 COMPLETED 支付单可退款，当前: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = now;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }
}
