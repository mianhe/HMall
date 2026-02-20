package com.hmall.cart.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, Long> {

    Optional<CartEntity> findByUserId(Long userId);
}
