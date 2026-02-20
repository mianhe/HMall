package com.hmall.payment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByPaymentId(Long paymentId);

    /** 查询状态为 PENDING 且 expiredAt <= before 的支付单（用于超时检测）。 */
    List<Payment> findPendingWithExpiredAtBefore(Instant before);

    /** 查询所有 PENDING 状态的支付单（用于批量重算超时时间）。 */
    List<Payment> findAllPending();
}
