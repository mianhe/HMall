package com.hmall.inventory.infrastructure;

import com.hmall.inventory.domain.Reservation;
import com.hmall.inventory.domain.ReservationRepository;
import com.hmall.inventory.domain.ReservationStatus;
import com.hmall.inventory.infrastructure.persistence.ReservationEntity;
import com.hmall.inventory.infrastructure.persistence.ReservationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReservationRepositoryImpl implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;

    public ReservationRepositoryImpl(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Reservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status) {
        return jpaRepository.findByOrderIdAndStatus(orderId, status).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = toEntity(reservation);
        ReservationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsByOrderIdAndStatus(Long orderId, ReservationStatus status) {
        return jpaRepository.existsByOrderIdAndStatus(orderId, status);
    }

    private ReservationEntity toEntity(Reservation domain) {
        ReservationEntity e = new ReservationEntity();
        if (domain.getReservationId() != null) {
            e.setId(domain.getReservationId());
        }
        e.setOrderId(domain.getOrderId());
        e.setSkuId(domain.getSkuId());
        e.setQuantity(domain.getQuantity());
        e.setStatus(domain.getStatus());
        e.setCreatedAt(domain.getCreatedAt());
        return e;
    }

    private Reservation toDomain(ReservationEntity entity) {
        return new Reservation(
            entity.getId(),
            entity.getOrderId(),
            entity.getSkuId(),
            entity.getQuantity(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
