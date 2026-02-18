package com.hmall.payment.infrastructure.persistence;

import com.hmall.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByStatusAndExpiredAtBefore(PaymentStatus status, Instant expiredAt);
}
