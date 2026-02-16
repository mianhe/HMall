package com.hmall.inventory.infrastructure.persistence;

import com.hmall.inventory.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {

    List<ReservationEntity> findByOrderIdAndStatus(Long orderId, ReservationStatus status);

    boolean existsByOrderIdAndStatus(Long orderId, ReservationStatus status);
}
