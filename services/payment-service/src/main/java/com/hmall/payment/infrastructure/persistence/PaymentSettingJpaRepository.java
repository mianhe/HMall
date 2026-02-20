package com.hmall.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettingJpaRepository extends JpaRepository<PaymentSettingEntity, String> {
}
