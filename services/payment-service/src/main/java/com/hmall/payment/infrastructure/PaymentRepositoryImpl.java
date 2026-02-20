package com.hmall.payment.infrastructure;

import com.hmall.payment.domain.Payment;
import com.hmall.payment.domain.PaymentRepository;
import com.hmall.payment.domain.PaymentStatus;
import com.hmall.payment.infrastructure.persistence.PaymentEntity;
import com.hmall.payment.infrastructure.persistence.PaymentJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    public PaymentRepositoryImpl(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = toEntity(payment);
        PaymentEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByPaymentId(Long paymentId) {
        return jpaRepository.findById(paymentId)
            .map(this::toDomain);
    }

    @Override
    public List<Payment> findPendingWithExpiredAtBefore(Instant before) {
        return jpaRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, before)
            .stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Payment> findAllPending() {
        return jpaRepository.findByStatus(PaymentStatus.PENDING)
            .stream()
            .map(this::toDomain)
            .toList();
    }

    private Payment toDomain(PaymentEntity e) {
        return Payment.reconstitute(
            e.getId(),
            e.getOrderId(),
            e.getAmountCents(),
            e.getStatus(),
            e.getPayUrl(),
            e.getCreatedAt(),
            e.getUpdatedAt(),
            e.getExpiredAt()
        );
    }

    private PaymentEntity toEntity(Payment payment) {
        PaymentEntity e = new PaymentEntity();
        if (payment.getPaymentId() != null) {
            e.setId(payment.getPaymentId());
        }
        e.setOrderId(payment.getOrderId());
        e.setAmountCents(payment.getAmountCents());
        e.setStatus(payment.getStatus());
        e.setPayUrl(payment.getPayUrl());
        e.setCreatedAt(payment.getCreatedAt());
        e.setUpdatedAt(payment.getUpdatedAt());
        e.setExpiredAt(payment.getExpiredAt());
        return e;
    }
}
